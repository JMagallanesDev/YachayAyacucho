package com.huamanga.tourism.lugar.dto;

import com.huamanga.tourism.lugar.domain.EstadoLugar;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Version reducida para el listado (RF-01).
 *
 * <p>Deliberadamente sin historia, consejos ni horarios: un listado paginado
 * de lugares no necesita el texto completo de cada ficha, y enviarlo
 * multiplicaria el peso de la respuesta sin que nadie lo lea (RNF-02).</p>
 */
@Schema(description = "Lugar en su version resumida, para listados")
public record LugarResumenResponse(

        UUID id,
        String slug,
        String nombre,
        String descripcion,
        LugarDetalleResponse.CategoriaResponse categoria,

        @Schema(example = "-74.2236")
        double longitud,

        @Schema(example = "-13.1588")
        double latitud,

        BigDecimal precioEntradaPen,
        Short duracionVisitaMin,
        EstadoLugar estado
) {
}
