package com.huamanga.tourism.moderacion.dto;

import com.huamanga.tourism.resena.domain.EstadoResena;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Reseña en la bandeja de moderacion (RF-50).
 *
 * @param editada true si el autor la cambio despues de publicarla. Es el dato
 *                que cierra el hueco de "escribo algo correcto y luego lo
 *                sustituyo": la bandeja las ordena primero para que se vuelvan
 *                a mirar
 */
@Schema(description = "Reseña revisable por un administrador")
public record ResenaModeracionResponse(
        UUID id,
        short calificacion,
        String comentario,
        String autor,
        String lugarSlug,
        EstadoResena estado,
        Instant creadaEn,
        boolean editada
) {
}
