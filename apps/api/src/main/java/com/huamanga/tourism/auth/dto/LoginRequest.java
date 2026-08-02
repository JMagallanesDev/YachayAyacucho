package com.huamanga.tourism.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de acceso (RF-32).
 *
 * <p>Sin validacion de formato a proposito: aqui no se valida, se
 * autentica. Decirle a quien intenta entrar que "el correo no tiene formato
 * valido" es informacion gratuita.</p>
 */
@Schema(description = "Credenciales de acceso")
public record LoginRequest(

        @Schema(example = "turista@ejemplo.pe")
        @NotBlank(message = "El correo es obligatorio")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {

    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
