package com.huamanga.tourism.seguridad;

import com.huamanga.tourism.auth.config.PropiedadesSeguridad;
import com.huamanga.tourism.common.seguridad.ResolutorIpCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lectura validada de X-Forwarded-For (RNF-14).
 *
 * <p>Este es el test que decide si el rate limiting sirve para algo. Si la
 * cabecera se leyera a ciegas, cualquiera enviaria una IP distinta en cada
 * peticion y tendria contadores infinitos: el limite existiria en el codigo
 * pero no en la practica.</p>
 */
@DisplayName("Resolucion de la IP real del cliente")
class ResolutorIpClienteTest {

    private final ResolutorIpCliente resolutor = new ResolutorIpCliente(propiedadesCon(
            List.of("10.0.0.0/8", "192.168.0.0/16", "127.0.0.1/32")));

    @Test
    @DisplayName("ignora X-Forwarded-For cuando la conexion NO viene de un proxy confiable")
    void ignoraCabeceraDePeerNoConfiable() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        // Un atacante conectando directo desde internet...
        peticion.setRemoteAddr("203.0.113.50");
        // ...que se inventa una IP para saltarse el contador.
        peticion.addHeader("X-Forwarded-For", "1.2.3.4");

        // Se usa la IP real de la conexion; la cabecera se descarta entera.
        assertThat(resolutor.resolver(peticion)).isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("cambiar la cabecera no permite esquivar el contador")
    void cabeceraFalsificadaNoCambiaLaClave() {
        String primera = resolver("203.0.113.50", "10.10.10.10");
        String segunda = resolver("203.0.113.50", "20.20.20.20");
        String tercera = resolver("203.0.113.50", "30.30.30.30");

        // Tres cabeceras distintas, una sola identidad: el limite se aplica.
        assertThat(primera).isEqualTo(segunda).isEqualTo(tercera).isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("respeta X-Forwarded-For cuando el proxy si es confiable")
    void respetaCabeceraDeProxyConfiable() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr("10.0.0.5");
        peticion.addHeader("X-Forwarded-For", "198.51.100.7");

        assertThat(resolutor.resolver(peticion)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("en una cadena de proxies toma la ultima IP no confiable, no la primera")
    void tomaLaIpCorrectaDeLaCadena() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr("10.0.0.5");
        // La primera entrada la escribio el cliente y puede ser mentira;
        // la ultima no confiable es la que anadio nuestro propio proxy.
        peticion.addHeader("X-Forwarded-For", "1.1.1.1, 198.51.100.7, 10.0.0.5");

        assertThat(resolutor.resolver(peticion)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("sin cabecera usa la IP de la conexion")
    void sinCabeceraUsaLaConexion() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr("10.0.0.5");

        assertThat(resolutor.resolver(peticion)).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("una cabecera con basura no rompe la resolucion")
    void cabeceraConBasuraNoRompe() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr("10.0.0.5");
        peticion.addHeader("X-Forwarded-For", "no-es-una-ip, , 198.51.100.7");

        assertThat(resolutor.resolver(peticion)).isEqualTo("198.51.100.7");
    }

    private String resolver(String peer, String cabecera) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr(peer);
        peticion.addHeader("X-Forwarded-For", cabecera);
        return resolutor.resolver(peticion);
    }

    private static PropiedadesSeguridad propiedadesCon(List<String> proxies) {
        return new PropiedadesSeguridad(
                "clave-de-prueba-suficientemente-larga-para-hs256",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                false,
                "Lax",
                proxies,
                "yachay-ayacucho");
    }
}
