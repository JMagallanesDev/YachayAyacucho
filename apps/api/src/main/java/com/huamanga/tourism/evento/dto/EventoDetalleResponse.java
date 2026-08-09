package com.huamanga.tourism.evento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Ficha completa de un evento (RF-80), con su clima (RF-88).
 *
 * <p>Compone el resumen en lugar de repetir sus quince campos: el calendario y
 * la ficha muestran lo mismo, y la ficha anade el clima y el rastro del
 * clonado.</p>
 */
@Schema(description = "Ficha de un evento con su clima")
public record EventoDetalleResponse(

        EventoResumenResponse evento,

        ClimaEventoResponse clima,

        @Schema(description = "Edicion anterior de la que se clono, si la hay")
        UUID eventoOrigenId
) {
}
