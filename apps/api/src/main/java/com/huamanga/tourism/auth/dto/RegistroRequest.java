package com.huamanga.tourism.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de un usuario nuevo (RF-31).
 */
@Schema(description = "Datos de registro de un usuario")
public record RegistroRequest(

        @Schema(example = "turista@ejemplo.pe")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
        String email,

        // La politica se declara aqui y se prueba en los tests. El mensaje
        // explica que falta sin revelar nada del sistema.
        @Schema(description = "Minimo 8 caracteres, con mayuscula, minuscula y digito")
        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contrasena debe incluir al menos una mayuscula, una minuscula y un digito")
        String password,

        @Schema(example = "Maria Quispe")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre
) {

    /**
     * El limite de 72 caracteres no es arbitrario: BCrypt ignora todo lo que
     * pase de 72 bytes. Sin este tope, dos contrasenas largas que compartan
     * los primeros 72 caracteres serian equivalentes para el sistema.
     */
    public RegistroRequest {
        email = email == null ? null : email.trim().toLowerCase();
        nombre = nombre == null ? null : nombre.trim();
    }
}
