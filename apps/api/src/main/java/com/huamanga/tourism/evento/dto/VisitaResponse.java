package com.huamanga.tourism.evento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * "Durante mi visita" (RF-84b): que ocurre en Huamanga entre dos fechas.
 *
 * <p>Los eventos son los que <strong>se solapan</strong> con el viaje, no los
 * que caen enteros dentro: una festividad que empezo el lunes y termina el
 * jueves le interesa a quien llega el miercoles. Es el caso que mas se olvida
 * al programar un cruce de fechas.</p>
 */
@Schema(description = "Eventos y clima del rango de fechas de un viaje")
public record VisitaResponse(

        LocalDate desde,

        LocalDate hasta,

        @Schema(description = "Un elemento por dia del viaje, en orden")
        List<DiaDeViaje> dias,

        @Schema(description = "Eventos que se solapan con el viaje")
        List<EventoResumenResponse> eventos
) {

    @Schema(description = "Un dia del viaje con el clima que se pueda decir de el")
    public record DiaDeViaje(

            LocalDate fecha,

            @Schema(description = "Clima, o el motivo por el que aun no lo hay")
            ClimaEventoResponse clima,

            @Schema(description = "Identificadores de los eventos activos ese dia")
            List<String> eventoIds
    ) {
    }
}
