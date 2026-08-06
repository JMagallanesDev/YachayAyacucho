package com.huamanga.tourism.moderacion.dto;

import com.huamanga.tourism.foto.domain.EstadoFoto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Foto pendiente de revision (RF-49).
 *
 * <p>Aqui la URL va <strong>sin transformar</strong>: quien modera necesita ver
 * la imagen tal cual se subio, no una version recortada y recomprimida que
 * podria ocultar justo lo que hay que juzgar.</p>
 */
@Schema(description = "Foto en la bandeja de moderacion")
public record FotoModeracionResponse(
        UUID id,
        String url,
        String autor,
        String lugarSlug,
        EstadoFoto estado,
        Instant subidaEn
) {
}
