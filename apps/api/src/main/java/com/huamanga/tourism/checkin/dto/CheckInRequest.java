package com.huamanga.tourism.checkin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Posicion que envia el navegador al registrar una visita (RF-39).
 *
 * @param precision el {@code accuracy} de la API de geolocalizacion, en metros.
 *                  Es opcional porque no todos los navegadores lo entregan;
 *                  cuando llega, se usa para descartar lecturas malas
 */
@Schema(description = "Posicion del visitante al hacer check-in")
public record CheckInRequest(

        @Schema(example = "-74.2236")
        @NotNull(message = "{checkin.longitud.obligatoria}")
        @DecimalMin(value = "-180.0", message = "{checkin.longitud.rango}")
        @DecimalMax(value = "180.0", message = "{checkin.longitud.rango}")
        Double longitud,

        @Schema(example = "-13.1588")
        @NotNull(message = "{checkin.latitud.obligatoria}")
        @DecimalMin(value = "-90.0", message = "{checkin.latitud.rango}")
        @DecimalMax(value = "90.0", message = "{checkin.latitud.rango}")
        Double latitud,

        @Schema(description = "Error estimado de la lectura, en metros", example = "18.5")
        Double precision
) {
}
