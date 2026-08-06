package com.huamanga.tourism.foto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Credenciales de Cloudinary, leidas de una sola variable.
 *
 * <p>Cloudinary entrega sus credenciales en el formato
 * {@code cloudinary://api_key:api_secret@cloud_name}, que es lo que se pega tal
 * cual en {@code CLOUDINARY_URL}. Tener las tres partes en una unica variable
 * evita el fallo tipico de rotar la clave y olvidarse del secreto.</p>
 *
 * <p>El {@code apiSecret} <strong>no sale nunca de esta clase y del cliente</strong>:
 * no aparece en DTOs, ni en logs, ni en mensajes de error. Solo se usa para
 * calcular firmas.</p>
 *
 * @param url         cadena completa; puede venir vacia
 * @param carpetaBase prefijo de todos los {@code public_id}, para no mezclar
 *                    con otros proyectos de la misma cuenta
 */
@ConfigurationProperties(prefix = "cloudinary")
public record PropiedadesCloudinary(String url, String carpetaBase) {

    private static final Pattern FORMATO =
            Pattern.compile("^cloudinary://([^:]+):([^@]+)@(.+)$");

    public PropiedadesCloudinary {
        carpetaBase = (carpetaBase == null || carpetaBase.isBlank())
                ? "yachay-ayacucho" : carpetaBase;
    }

    /** Sin credenciales el modulo se degrada en vez de impedir el arranque. */
    public boolean configurado() {
        return url != null && FORMATO.matcher(url.trim()).matches();
    }

    public String apiKey() {
        return parte(1);
    }

    public String apiSecret() {
        return parte(2);
    }

    public String cloudName() {
        return parte(3);
    }

    private String parte(int grupo) {
        if (!configurado()) {
            throw new IllegalStateException(
                    "CLOUDINARY_URL no esta configurada o no tiene el formato "
                            + "cloudinary://api_key:api_secret@cloud_name");
        }
        Matcher m = FORMATO.matcher(url.trim());
        m.matches();
        return m.group(grupo);
    }
}
