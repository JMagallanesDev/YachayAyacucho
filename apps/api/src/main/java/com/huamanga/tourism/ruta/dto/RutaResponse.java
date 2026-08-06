package com.huamanga.tourism.ruta.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Ruta tematica lista para dibujarse como polilinea (RF-20).
 *
 * <p>Los puntos vienen <strong>en el orden de recorrido</strong>, que es la
 * columna {@code orden} de {@code lugar_ruta}. Ese orden es el dato con valor:
 * una ruta cultural no es un conjunto de lugares sino una secuencia, y unir los
 * puntos en otro orden dibujaria un garabato sobre la ciudad.</p>
 */
@Schema(description = "Ruta tematica con su recorrido")
public record RutaResponse(

        UUID id,
        String slug,
        String nombre,
        String descripcion,
        String colorHex,
        String icono,

        @Schema(description = "Paradas en orden de recorrido")
        List<Parada> paradas
) {

    @Schema(description = "Una parada de la ruta")
    public record Parada(
            UUID lugarId,
            String slug,
            String nombre,
            short orden,
            double longitud,
            double latitud
    ) {
    }
}
