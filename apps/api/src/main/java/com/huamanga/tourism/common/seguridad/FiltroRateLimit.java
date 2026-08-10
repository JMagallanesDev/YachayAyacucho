package com.huamanga.tourism.common.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * Rate limiting por IP con contador en Redis (RNF-14).
 *
 * <p>Ventana fija con {@code INCR} + {@code EXPIRE}: la primera peticion de
 * cada ventana crea la clave y le pone caducidad, y el resto solo incrementa.
 * Al vivir en Redis, el limite es <strong>compartido entre todas las
 * instancias</strong> del backend; un contador en memoria se multiplicaria
 * por el numero de replicas y dejaria de significar nada.</p>
 *
 * <p>Los endpoints de autenticacion llevan un limite mucho mas bajo, porque
 * es donde se hace fuerza bruta contra contrasenas. 100 intentos por minuto
 * contra un login son demasiados.</p>
 *
 * <p><strong>El servidor de Next esta exento (Bloque 13).</strong> Al generar
 * las paginas estaticas, el frontend pide al API los 15 lugares, los eventos y
 * los negocios en dos idiomas: mas de cien llamadas en unos segundos y desde una
 * sola direccion, que es exactamente el patron que este filtro existe para
 * frenar. El resultado era que {@code next build} abortaba con un 429.</p>
 *
 * <p>La exencion NO se hace por IP. Adivinar «esto viene de dentro» mirando la
 * direccion se rompe en cuanto hay un proxy delante, y en el despliegue previsto
 * —Vercel llamando a Railway— el frontend sale con IP publica, asi que no habria
 * nada que reconocer. En su lugar hay un <strong>secreto compartido</strong> en
 * una cabecera: es una relacion de confianza explicita, funciona con cualquier
 * topologia, y el secreto vive solo en el servidor —sin prefijo
 * {@code NEXT_PUBLIC_}— de modo que jamas llega al navegador.</p>
 *
 * <p>Si el secreto no esta configurado, no se exime a nadie: falla cerrando.</p>
 */
@Component
public class FiltroRateLimit extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroRateLimit.class);

    private static final Duration VENTANA = Duration.ofMinutes(1);
    private static final int LIMITE_GENERAL = 100;
    private static final int LIMITE_AUTENTICACION = 10;

    private static final String PREFIJO_CLAVE = "rate:";

    /** Cabecera con la que el servidor de Next se identifica. */
    private static final String CABECERA_INTERNA = "X-Yachay-Interno";

    private final StringRedisTemplate redis;
    private final ResolutorIpCliente resolutorIp;

    /** Vacio cuando no se configuro: entonces no se exime a nadie. */
    private final byte[] secretoInterno;

    public FiltroRateLimit(StringRedisTemplate redis,
                           ResolutorIpCliente resolutorIp,
                           @Value("${app.seguridad.token-interno:}") String tokenInterno) {
        this.redis = redis;
        this.resolutorIp = resolutorIp;
        this.secretoInterno = tokenInterno == null || tokenInterno.isBlank()
                ? new byte[0]
                : tokenInterno.getBytes(StandardCharsets.UTF_8);

        if (this.secretoInterno.length == 0) {
            log.info("Sin token interno: el rate limit se aplica a todas las peticiones, "
                    + "incluidas las del propio servidor de Next durante el build");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        if (esLlamadaInterna(peticion)) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        String ruta = peticion.getRequestURI();
        boolean esAutenticacion = ruta.contains("/auth/login") || ruta.contains("/auth/register");
        int limite = esAutenticacion ? LIMITE_AUTENTICACION : LIMITE_GENERAL;

        String ip = resolutorIp.resolver(peticion);
        String clave = PREFIJO_CLAVE + (esAutenticacion ? "auth:" : "general:") + ip;

        Long usos = contar(clave);

        // Fail-open deliberado: si Redis no responde, `contar` devuelve null y
        // la peticion sigue. Degradar el rate limiting es preferible a dejar
        // el sistema entero inaccesible por una caida de la cache.
        if (usos != null && usos > limite) {
            log.warn("Rate limit superado por {} en {} ({} peticiones)", ip, ruta, usos);
            responder429(respuesta);
            return;
        }

        cadena.doFilter(peticion, respuesta);
    }

    /**
     * ¿La peticion viene del propio servidor de Next?
     *
     * <p>La comparacion es en <strong>tiempo constante</strong>: comparar con
     * {@code equals} termina en el primer byte distinto, y esa diferencia de
     * tiempo es medible desde fuera. Con suficientes intentos permite deducir
     * el secreto caracter a caracter, que es el ataque clasico contra una
     * comparacion ingenua de credenciales.</p>
     */
    private boolean esLlamadaInterna(HttpServletRequest peticion) {
        if (secretoInterno.length == 0) {
            return false;
        }
        String recibido = peticion.getHeader(CABECERA_INTERNA);
        if (recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(recibido.getBytes(StandardCharsets.UTF_8), secretoInterno);
    }

    private Long contar(String clave) {
        try {
            Long usos = redis.opsForValue().increment(clave);
            if (usos != null && usos == 1L) {
                redis.expire(clave, VENTANA);
            }
            return usos;
        } catch (RuntimeException ex) {
            log.error("Redis no disponible; el rate limiting queda desactivado en esta peticion", ex);
            return null;
        }
    }

    private void responder429(HttpServletResponse respuesta) throws IOException {
        respuesta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(VENTANA.toSeconds()));
        respuesta.getWriter().write("""
                {"type":"https://yachay-ayacucho.pe/errores/limite-peticiones",\
                "title":"Demasiadas peticiones",\
                "status":429,\
                "detail":"Has hecho demasiadas peticiones. Espera un momento e intentalo de nuevo.",\
                "errorCode":"limite-peticiones"}""");
    }
}
