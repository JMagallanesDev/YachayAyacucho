package com.huamanga.tourism.foto.dto;

import com.huamanga.tourism.foto.domain.EstadoFoto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Foto de un lugar.
 *
 * <p>El {@code public_id} de Cloudinary <strong>no viaja</strong>: es la llave
 * para borrar el binario del CDN y no tiene por que salir del servidor. Al
 * publico le basta la URL.</p>
 */
@Schema(description = "Foto de un lugar")
public record FotoResponse(

        UUID id,

        @Schema(description = "URL optimizada, servida por el CDN de Cloudinary")
        String url,

        @Schema(description = "Miniatura para la rejilla de la galeria")
        String miniatura,

        @Schema(description = "Quien la subio")
        String autor,

        EstadoFoto estado,

        @Schema(description = "Solo se rellena cuando la foto fue rechazada")
        String motivoRechazo,

        Instant subidaEn
) {
}
