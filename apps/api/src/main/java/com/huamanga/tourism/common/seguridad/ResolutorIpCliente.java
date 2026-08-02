package com.huamanga.tourism.common.seguridad;

import com.huamanga.tourism.auth.config.PropiedadesSeguridad;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Determina la IP real del cliente para el rate limiting (RNF-14).
 *
 * <p><strong>El detalle que hace util a todo esto.</strong>
 * {@code X-Forwarded-For} la escribe quien envia la peticion, asi que leerla
 * a ciegas convierte el rate limiting en decoracion: basta con mandar una IP
 * inventada distinta en cada intento para tener contadores infinitos. Un
 * ataque de fuerza bruta contra el login pasaria sin tocar el limite.</p>
 *
 * <p>La lectura correcta tiene dos reglas:</p>
 * <ol>
 *   <li>La cabecera solo se mira si <em>la conexion directa</em> viene de un
 *       proxy en el que confiamos (Railway, Vercel, un balanceador propio).
 *       De cualquier otro origen se ignora por completo.</li>
 *   <li>Dentro de la cadena se recorre <em>de derecha a izquierda</em>,
 *       descartando proxies conocidos. La primera entrada es la que el
 *       cliente pudo falsificar; la ultima no confiable es la real.</li>
 * </ol>
 */
@Component
public class ResolutorIpCliente {

    private static final Logger log = LoggerFactory.getLogger(ResolutorIpCliente.class);
    private static final String CABECERA_XFF = "X-Forwarded-For";

    private final List<RangoCidr> proxiesConfiables;

    public ResolutorIpCliente(PropiedadesSeguridad propiedades) {
        this.proxiesConfiables = propiedades.proxiesConfiables().stream()
                .map(RangoCidr::parsear)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public String resolver(HttpServletRequest peticion) {
        String ipDirecta = peticion.getRemoteAddr();

        // Regla 1: si quien nos habla no es un proxy de confianza, su IP es la
        // unica verdad disponible y la cabecera se descarta.
        if (!esProxyConfiable(ipDirecta)) {
            return ipDirecta;
        }

        String cabecera = peticion.getHeader(CABECERA_XFF);
        if (cabecera == null || cabecera.isBlank()) {
            return ipDirecta;
        }

        // Regla 2: de derecha a izquierda hasta encontrar algo que no sea un
        // proxy nuestro.
        List<String> cadena = new ArrayList<>(List.of(cabecera.split(",")));
        for (int i = cadena.size() - 1; i >= 0; i--) {
            String candidata = cadena.get(i).trim();
            if (candidata.isEmpty()) {
                continue;
            }
            if (!esProxyConfiable(candidata)) {
                return candidata;
            }
        }

        return ipDirecta;
    }

    private boolean esProxyConfiable(String ip) {
        try {
            byte[] direccion = InetAddress.getByName(ip).getAddress();
            return proxiesConfiables.stream().anyMatch(rango -> rango.contiene(direccion));
        } catch (UnknownHostException ex) {
            // Una entrada que ni siquiera es una IP valida jamas es de fiar.
            log.debug("Valor no interpretable como IP: {}", ip);
            return false;
        }
    }

    /** Rango CIDR con comparacion bit a bit, valido para IPv4 e IPv6. */
    private record RangoCidr(byte[] red, int bitsPrefijo) {

        static RangoCidr parsear(String notacion) {
            try {
                String[] partes = notacion.trim().split("/");
                byte[] red = InetAddress.getByName(partes[0]).getAddress();
                int bits = partes.length > 1 ? Integer.parseInt(partes[1]) : red.length * 8;
                return new RangoCidr(red, bits);
            } catch (UnknownHostException | NumberFormatException ex) {
                LoggerFactory.getLogger(ResolutorIpCliente.class)
                        .warn("Rango de proxy confiable mal formado, se ignora: {}", notacion);
                return null;
            }
        }

        boolean contiene(byte[] direccion) {
            // IPv4 e IPv6 no se comparan entre si.
            if (direccion.length != red.length) {
                return false;
            }
            int bytesCompletos = bitsPrefijo / 8;
            int bitsSueltos = bitsPrefijo % 8;

            for (int i = 0; i < bytesCompletos; i++) {
                if (direccion[i] != red[i]) {
                    return false;
                }
            }
            if (bitsSueltos == 0) {
                return true;
            }
            int mascara = (0xFF00 >> bitsSueltos) & 0xFF;
            return (direccion[bytesCompletos] & mascara) == (red[bytesCompletos] & mascara);
        }
    }
}
