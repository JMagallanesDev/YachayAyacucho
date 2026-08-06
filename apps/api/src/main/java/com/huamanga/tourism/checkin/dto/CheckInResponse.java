package com.huamanga.tourism.checkin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Resultado de una visita registrada.
 *
 * @param insigniasNuevas codigos de las insignias recien obtenidas. Van en la
 *                        respuesta para poder celebrarlas en el momento: si el
 *                        cliente tuviera que volver a pedir el pasaporte para
 *                        enterarse, el logro llegaria tarde y sin gracia
 */
@Schema(description = "Visita registrada y sellos obtenidos")
public record CheckInResponse(

        UUID id,
        String lugarSlug,

        @Schema(description = "Distancia real al lugar en el momento del check-in, en metros")
        long distanciaMetros,

        @Schema(description = "Sellos del pasaporte: lugares distintos visitados")
        long sellos,

        List<String> insigniasNuevas
) {
}
