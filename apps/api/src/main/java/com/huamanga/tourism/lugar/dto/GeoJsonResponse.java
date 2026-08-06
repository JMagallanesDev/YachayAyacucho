package com.huamanga.tourism.lugar.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Lugares en GeoJSON, listos para MapLibre (RF-17, RF-18).
 *
 * <p>Se devuelve GeoJSON y no la lista normal por una razon concreta: MapLibre
 * consume una fuente GeoJSON <strong>directamente</strong>, y con
 * {@code cluster: true} agrupa los puntos dentro de la GPU. Ese es el camino que
 * permite cumplir el RNF-04 (mapa en menos de 2 s y mas de 30 FPS con 200
 * marcadores): pintar 200 marcadores como elementos del DOM hunde los
 * fotogramas.</p>
 *
 * <p>Cada punto lleva solo lo que la chincheta necesita. La ficha completa se
 * pide al abrirla.</p>
 */
@Schema(description = "FeatureCollection de GeoJSON con los lugares publicados")
public record GeoJsonResponse(String type, List<Feature> features) {

    public static GeoJsonResponse de(List<Feature> features) {
        return new GeoJsonResponse("FeatureCollection", features);
    }

    @Schema(description = "Un lugar como punto de GeoJSON")
    public record Feature(String type, Geometry geometry, Propiedades properties) {

        public static Feature punto(double longitud, double latitud, Propiedades propiedades) {
            return new Feature("Feature", Geometry.punto(longitud, latitud), propiedades);
        }
    }

    /**
     * @param coordinates en GeoJSON el orden es SIEMPRE [longitud, latitud].
     *                    Invertirlo es el error clasico y colocaria Huamanga en
     *                    mitad del oceano Indico
     */
    public record Geometry(String type, double[] coordinates) {

        public static Geometry punto(double longitud, double latitud) {
            return new Geometry("Point", new double[]{longitud, latitud});
        }
    }

    @Schema(description = "Datos de la chincheta")
    public record Propiedades(
            String id,
            String slug,
            String nombre,
            String categoriaCodigo,
            String categoriaNombre,
            String color,
            String icono
    ) {
    }
}
