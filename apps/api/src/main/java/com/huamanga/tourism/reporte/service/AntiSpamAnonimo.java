package com.huamanga.tourism.reporte.service;

import com.huamanga.tourism.common.seguridad.HuellaAnonima;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Limite de reportes anonimos sin guardar quien los envia (RF-72).
 *
 * <p><strong>La tension que resuelve.</strong> Para frenar el abuso de un
 * formulario anonimo hace falta algun identificador, y el unico disponible es
 * la direccion IP. Guardarla —aunque sea en Redis y con caducidad— crearia
 * durante 24 h un rastro que asocia una IP con haber denunciado algo. En un
 * modulo cuyo proposito es que alguien pueda acusar a un tercero de danar el
 * patrimonio sin miedo, ese rastro no es un detalle.</p>
 *
 * <p>La parte criptografica vive en {@link HuellaAnonima}, que desde el Bloque
 * 10 comparte con la analitica de trafico: la IP se convierte en un
 * {@code HMAC-SHA256} bajo una sal que solo existe en memoria y rota a diario,
 * de modo que ni volcando Redis se puede recuperar el origen.</p>
 */
@Component
public class AntiSpamAnonimo {

    private static final Logger log = LoggerFactory.getLogger(AntiSpamAnonimo.class);

    /** Reportes por dia y origen. Denunciar cinco cosas distintas en un dia ya es mucho. */
    public static final int MAXIMO_POR_DIA = 5;

    private static final Duration VENTANA = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final HuellaAnonima huellas;

    public AntiSpamAnonimo(StringRedisTemplate redis, HuellaAnonima huellas) {
        this.redis = redis;
        this.huellas = huellas;
    }

    /**
     * @throws DemasiadosReportesException si el origen agoto su cupo diario
     */
    public void comprobar(HttpServletRequest peticion) {
        String clave = "reporte:anon:" + huellas.de(peticion);

        try {
            Long usos = redis.opsForValue().increment(clave);
            if (usos == null) {
                return;
            }
            // La caducidad se fija solo en el primer uso: renovarla en cada
            // intento convertiria la ventana en una prision que nunca expira
            // mientras se siga intentando.
            if (usos == 1L) {
                redis.expire(clave, VENTANA);
            }
            if (usos > MAXIMO_POR_DIA) {
                throw new DemasiadosReportesException(MAXIMO_POR_DIA);
            }
        } catch (DemasiadosReportesException e) {
            throw e;
        } catch (Exception e) {
            // Falla abriendo: es preferible aceptar un reporte de mas a que una
            // denuncia legitima se pierda porque Redis no responde.
            log.warn("Anti-spam de reportes sin Redis, se deja pasar: {}", e.getMessage());
        }
    }

    public static class DemasiadosReportesException extends RuntimeException {
        public DemasiadosReportesException(int maximo) {
            super("Se alcanzo el limite de " + maximo + " reportes en 24 horas");
        }
    }
}
