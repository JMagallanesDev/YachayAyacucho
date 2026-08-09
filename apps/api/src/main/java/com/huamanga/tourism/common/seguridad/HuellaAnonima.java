package com.huamanga.tourism.common.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Identificador efimero de un origen, del que no se puede recuperar la IP.
 *
 * <p>Nacio dentro del anti-spam de reportes anonimos del Bloque 8 y vive aqui
 * desde el Bloque 10, cuando la analitica de trafico necesito exactamente lo
 * mismo. Tener dos implementaciones del mismo mecanismo criptografico en dos
 * paquetes distintos era la forma segura de que una de las dos se degradara con
 * el tiempo sin que nadie lo notara.</p>
 *
 * <p><strong>Por que no basta con hashear la IP.</strong> Un SHA-256 de una
 * direccion IPv4 es reversible en minutos: el espacio entero son unos 4.300
 * millones de valores y se recorre con un equipo domestico. Un hash no anonimiza
 * un dominio pequeno y conocido; solo lo disfraza.</p>
 *
 * <p><strong>Lo que se hace en su lugar.</strong> {@code HMAC-SHA256(ip, sal)}
 * con una <em>sal aleatoria de 32 bytes generada en memoria que rota cada
 * dia</em> y que no se persiste en ningun sitio: ni en el {@code .env}, ni en la
 * base, ni en un archivo. Quien vuelque Redis obtiene cadenas inservibles, y la
 * sal solo existe en la memoria del proceso. Al rotar a diario, tampoco se
 * pueden correlacionar las huellas de un dia con las del siguiente.</p>
 *
 * <p><strong>El precio, dicho de frente.</strong> Si el backend se reinicia, la
 * sal cambia y todo lo que contaba por huella empieza de cero. Es aceptable en
 * los dos usos que tiene: un freno al abuso y un contador de visitas. No lo
 * seria para un control de seguridad, y por eso no se usa como tal.</p>
 */
@Component
public class HuellaAnonima {

    private static final String ALGORITMO = "HmacSHA256";

    /** 16 bytes bastan contra colisiones en estos usos y acortan las claves. */
    private static final int BYTES_DE_HUELLA = 16;

    private final ResolutorIpCliente resolutorIp;

    /** Sal vigente y el dia al que pertenece, para saber cuando rotarla. */
    private final AtomicReference<SalDelDia> sal = new AtomicReference<>(SalDelDia.nueva());

    public HuellaAnonima(ResolutorIpCliente resolutorIp) {
        this.resolutorIp = resolutorIp;
    }

    /**
     * Huella del origen de una peticion.
     *
     * <p>Es lo unico que sale de esta clase: la IP no se devuelve, no se registra
     * y no se puede reconstruir a partir del resultado.</p>
     */
    public String de(HttpServletRequest peticion) {
        return calcular(resolutorIp.resolver(peticion));
    }

    /** Variante para pruebas y para cuando la IP ya se resolvio antes. */
    public String deIp(String ip) {
        return calcular(ip);
    }

    private String calcular(String ip) {
        SalDelDia actual = sal.updateAndGet(previa -> previa.esDeHoy() ? previa : SalDelDia.nueva());

        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(actual.bytes(), ALGORITMO));
            byte[] resumen = mac.doFinal(ip.getBytes(StandardCharsets.UTF_8));

            byte[] recortado = new byte[BYTES_DE_HUELLA];
            System.arraycopy(resumen, 0, recortado, 0, BYTES_DE_HUELLA);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(recortado);

        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular la huella anonima", e);
        }
    }

    /**
     * Sal de un dia concreto.
     *
     * <p>El dia se guarda junto a los bytes para poder rotar sin un job
     * programado: la primera peticion de cada jornada se encuentra con una sal
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
}
