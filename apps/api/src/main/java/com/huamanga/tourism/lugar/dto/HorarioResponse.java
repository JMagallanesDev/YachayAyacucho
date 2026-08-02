package com.huamanga.tourism.lugar.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

/** Una franja horaria tal como se devuelve al cliente. */
@Schema(description = "Franja horaria de un dia")
public record HorarioResponse(

        @Schema(description = "0 = domingo ... 6 = sabado", example = "1")
        Short diaSemana,

        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean cerrado
) {
}
