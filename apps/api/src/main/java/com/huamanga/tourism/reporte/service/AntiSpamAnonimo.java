package com.huamanga.tourism.reporte.service;

import com.huamanga.tourism.common.seguridad.ResolutorIpCliente;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

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
 * <p><strong>Por que no basta con hashear la IP.</strong> Un SHA-256 de una IP
 * es reversible en minutos: el espacio IPv4 completo son unos 4.300 millones de
 * valores y se recorre entero con un equipo domestico. Un hash no anonimiza un
 * dominio pequeno y conocido; solo lo disfraza.</p>
 *
 * <p><strong>La solucion.</strong> La clave de Redis es
 * {@code HMAC-SHA256(ip, sal)} con una <em>sal aleatoria de 32 bytes que se
 * genera en memoria y rota cada dia</em>, y que no se persiste en ningun sitio.
 * Quien vuelque Redis obtiene cadenas inservibles sin la sal, y la sal solo
 * existe en la memoria del proceso. Al rotar a diario, tampoco se pueden
 * correlacionar las claves de un dia con las del siguiente.</p>
 *
 * <p><strong>El precio.</strong> Si el backend se reinicia, la sal cambia y los
 * contadores se reinician con ella. Es aceptable: esto es un freno al abuso, no
 * un control de seguridad, y nadie deberia perder la posibilidad de denunciar
 * porque un servidor se reinicio.</p>
 */
@Component
public class AntiSpamAnonimo {

    private static final Logger log = LoggerFactory.getLogger(AntiSpamAnonimo.class);

    /** Reportes por dia y origen. Denunciar cinco cosas distintas en un dia ya es mucho. */
    public static final int MAXIMO_POR_DIA = 5;

    private static final Duration VENTANA = Duration.ofHours(24);
    private static final String ALGORITMO = "HmacSHA256";

    private final StringRedisTemplate redis;
    private final ResolutorIpCliente resolutorIp;

    /** Sal vigente y el dia al que pertenece, para saber cuando rotarla. */
    private final AtomicReference<SalDelDia> sal = new AtomicReference<>(SalDelDia.nueva());

    public AntiSpamAnonimo(StringRedisTemplate redis, ResolutorIpCliente resolutorIp) {
        this.redis = redis;
        this.resolutorIp = resolutorIp;
    }

    /**
     * @throws DemasiadosReportesException si el origen agoto su cupo diario
     */
    public void comprobar(HttpServletRequest peticion) {
        String clave = "reporte:anon:" + huella(resolutorIp.resolver(peticion));

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

    /**
     * Huella no reversible del origen.
     *
     * <p>Nunca devuelve la IP ni permite recuperarla. Es lo unico que llega a
     * salir de este metodo, y lo unico que se escribe en Redis.</p>
     */
    private String huella(String ip) {
        SalDelDia actual = sal.updateAndGet(
                previa -> previa.esDeHoy() ? previa : SalDelDia.nueva());

        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(actual.bytes(), ALGORITMO));
            byte[] resumen = mac.doFinal(ip.getBytes(StandardCharsets.UTF_8));

            // Se recorta a 16 bytes: suficiente contra colisiones para este uso
            // y mas corto en Redis.
            byte[] recortado = new byte[16];
            System.arraycopy(resumen, 0, recortado, 0, 16);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(recortado);

        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular la huella anonima", e);
        }
    }

    /**
     * Sal de un dia concreto.
     *
     * <p>Se guarda el dia junto a los bytes para poder rotar sin un job
     * programado: la primera peticion de cada dia se encuentra con una sal
     * caducada y la sustituye.</p>
     */
    private record SalDelDia(byte[] bytes, LocalDate dia) {

        static SalDelDia nueva() {
            byte[] aleatoria = new byte[32];
            new SecureRandom().nextBytes(aleatoria);
            return new SalDelDia(aleatoria, LocalDate.now(ZoneOffset.UTC));
        }

        boolean esDeHoy() {
            return dia.equals(LocalDate.now(ZoneOffset.UTC));
        }
    }

    public static class DemasiadosReportesException extends RuntimeException {
        public DemasiadosReportesException(int maximo) {
            super("Se alcanzo el limite de " + maximo + " reportes en 24 horas");
        }
    }
}
