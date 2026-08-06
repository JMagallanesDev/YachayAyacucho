package com.huamanga.tourism.common.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Limite de contenido por usuario (anti-spam).
 *
 * <p>Es distinto del {@code FiltroRateLimit} del Bloque 2, y por eso vive
 * aparte: aquel limita <strong>peticiones por IP</strong> para frenar la fuerza
 * bruta contra el login; este limita <strong>contenido creado por cuenta</strong>
 * para que nadie llene la base de reseñas o fotos. Una misma persona puede
 * cambiar de IP, y varias personas pueden compartirla.</p>
 *
 * <p>No se usa reCAPTCHA a proposito: para esta escala anade una dependencia de
 * Google en cada formulario, penaliza la accesibilidad y no aporta frente a un
 * limite por cuenta autenticada, que ya obliga a registrarse.</p>
 *
 * <p><strong>Falla abriendo.</strong> Si Redis no responde, se deja pasar: es
 * preferible a impedir que alguien opine porque la cache esta caida. El mismo
 * criterio que se acordo para el rate limiting del Bloque 2.</p>
 */
@Component
public class AntiSpam {

    private static final Logger log = LoggerFactory.getLogger(AntiSpam.class);

    /** Reseñas por hora y cuenta. Escribir 10 reseñas honestas en una hora ya es mucho. */
    private static final int RESENAS_POR_HORA = 10;

    /** Fotos por hora y cuenta. Cada una pasa por moderacion manual. */
    private static final int FOTOS_POR_HORA = 20;

    private static final Duration VENTANA = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    public AntiSpam(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void comprobarResena() {
        comprobar("resena", RESENAS_POR_HORA);
    }

    public void comprobarFoto() {
        comprobar("foto", FOTOS_POR_HORA);
    }

    private void comprobar(String accion, int maximo) {
        UUID usuarioId = UsuarioActual.idObligatorio();
        String clave = "antispam:%s:%s".formatted(accion, usuarioId);

        try {
            Long usos = redis.opsForValue().increment(clave);
            if (usos == null) {
                return;
            }
            // La expiracion se fija solo en el primer uso: renovarla en cada
            // peticion convertiria la ventana deslizante en una prision, porque
            // nunca llegaria a caducar mientras se siguiera intentando.
            if (usos == 1L) {
                redis.expire(clave, VENTANA);
            }
            if (usos > maximo) {
                throw new DemasiadasPeticionesException(accion);
            }
        } catch (DemasiadasPeticionesException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Anti-spam sin Redis, se deja pasar: {}", e.getMessage());
        }
    }

    public static class DemasiadasPeticionesException extends RuntimeException {
        public DemasiadasPeticionesException(String accion) {
            super("Demasiadas operaciones de tipo " + accion + " en poco tiempo");
        }
    }
}
