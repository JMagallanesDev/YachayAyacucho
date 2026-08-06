package com.huamanga.tourism.resena.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reseña que envía un visitante (RF-37).
 *
 * <p>Los limites repiten en Java los CHECK que ya impone PostgreSQL. No es
 * duplicacion inutil: la base es la ultima linea de defensa y devuelve un error
 * de integridad ilegible, mientras que Bean Validation responde un 400 con el
 * campo exacto y el mensaje traducido.</p>
 */
@Schema(description = "Calificacion y comentario de un lugar")
public record ResenaRequest(

        @Schema(description = "De 1 a 5 estrellas", example = "4")
        @NotNull(message = "{resena.calificacion.obligatoria}")
        @Min(value = 1, message = "{resena.calificacion.rango}")
        @Max(value = 5, message = "{resena.calificacion.rango}")
        Short calificacion,

        @Schema(description = "Comentario opcional, maximo 500 caracteres")
        @Size(max = 500, message = "{resena.comentario.largo}")
        String comentario
) {

    /** Un comentario en blanco equivale a no dejar comentario. */
    public String comentarioNormalizado() {
        return comentario == null || comentario.isBlank() ? null : comentario.trim();
    }
}
