package com.huamanga.tourism.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de un login o de una renovacion.
 *
 * <p>El refresh token <strong>no aparece aqui</strong>: viaja en una cookie
 * httpOnly que el JavaScript de la pagina no puede leer. Si se devolviera en
 * el cuerpo, cualquier XSS podria robarlo y con el mantener la sesion
 * indefinidamente.</p>
 */
@Schema(description = "Access token y datos de la sesion")
public record AutenticacionResponse(

        @Schema(description = "JWT de acceso. Se guarda solo en memoria, nunca en localStorage")
        String accessToken,

        @Schema(description = "Segundos que faltan para que caduque", example = "900")
        long expiraEnSegundos,

        UsuarioResponse usuario
) {
}
