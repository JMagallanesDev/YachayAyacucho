package com.huamanga.tourism.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuracion de seguridad, toda por variables de entorno (RNF-39).
 *
 * @param jwtSecret          clave HS256; minimo 32 bytes (256 bits)
 * @param accessExpiration   vida del access token
 * @param refreshExpiration  vida del refresh token
 * @param cookieSecure       marca Secure de la cookie; false solo en local
 * @param cookieSameSite     Lax en mismo sitio, None entre dominios distintos
 * @param proxiesConfiables  redes cuyo X-Forwarded-For se acepta
 * @param emisor             claim "iss" de los tokens emitidos
 */
@ConfigurationProperties(prefix = "app.seguridad")
public record PropiedadesSeguridad(
        String jwtSecret,
        Duration accessExpiration,
        Duration refreshExpiration,
        boolean cookieSecure,
        String cookieSameSite,
        List<String> proxiesConfiables,
        String emisor
) {

    /** Nombre de la cookie que transporta el refresh token. */
    public static final String COOKIE_REFRESH = "yachay_refresh";

    /**
     * Cabecera que /auth/refresh exige cuando la cookie es cross-site.
     *
     * <p>Es la defensa CSRF que compensa {@code SameSite=None}: un formulario
     * malicioso alojado en otro dominio puede provocar que el navegador envie
     * la cookie, pero <strong>no puede anadir cabeceras propias</strong> sin
     * disparar un preflight CORS que nuestro allowlist rechaza.</p>
     */
    public static final String CABECERA_ANTI_CSRF = "X-Refresh-Request";

    public PropiedadesSeguridad {
        if (jwtSecret == null || jwtSecret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 bytes: HS256 exige una clave de 256 bits");
        }
        proxiesConfiables = proxiesConfiables == null ? List.of() : List.copyOf(proxiesConfiables);
        emisor = emisor == null ? "yachay-ayacucho" : emisor;
    }
}
