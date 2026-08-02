package com.huamanga.tourism.common.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
 */
@Component
public class FiltroRateLimit extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroRateLimit.class);

    private static final Duration VENTANA = Duration.ofMinutes(1);
    private static final int LIMITE_GENERAL = 100;
    private static final int LIMITE_AUTENTICACION = 10;

    private static final String PREFIJO_CLAVE = "rate:";

    private final StringRedisTemplate redis;
    private final ResolutorIpCliente resolutorIp;

    public FiltroRateLimit(StringRedisTemplate redis, ResolutorIpCliente resolutorIp) {
        this.redis = redis;
        this.resolutorIp = resolutorIp;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
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
