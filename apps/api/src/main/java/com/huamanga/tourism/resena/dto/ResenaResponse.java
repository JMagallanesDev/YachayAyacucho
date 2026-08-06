package com.huamanga.tourism.resena.dto;

import com.huamanga.tourism.resena.domain.EstadoResena;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Reseña tal como la ve el publico.
 *
 * <p>Del autor solo salen su identificador y su nombre. El correo no viaja
 * nunca: es un dato personal y no aporta nada a quien lee una reseña.</p>
 */
@Schema(description = "Reseña publicada de un lugar")
public record ResenaResponse(

        UUID id,
        short calificacion,
        String comentario,

        @Schema(description = "Nombre del autor")
        String autor,

        @Schema(description = "Identificador del autor, para que el cliente reconozca la suya")
        UUID autorId,

        @Schema(description = "Estado de moderacion; el publico solo ve las PUBLICADA")
        EstadoResena estado,

        Instant creadaEn,

        @Schema(description = "True si el autor la modifico despues de publicarla")
        boolean editada
) {
}
