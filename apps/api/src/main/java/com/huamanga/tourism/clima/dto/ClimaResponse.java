package com.huamanga.tourism.clima.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Clima actual de Huamanga (RF-25).
 *
 * <p>Los tres campos del final son el contrato de honestidad de este modulo.
 * Un clima puede llegar al navegador en tres estados distintos y la interfaz
 * necesita distinguirlos:</p>
 * <ul>
 *   <li><strong>Fresco:</strong> {@code disponible=true}, {@code obsoleto=false}.</li>
 *   <li><strong>Obsoleto:</strong> {@code obsoleto=true} — el proveedor no
 *       responde y esto es lo ultimo que se supo, medido en {@code medidoEn}.
 *       Se muestra con su antiguedad («hace 2 h»), nunca disfrazado de
 *       actual.</li>
 *   <li><strong>Sin dato:</strong> {@code disponible=false} — no hay nada que
 *       mostrar. La ficha y el mapa siguen funcionando: el clima informa, no
 *       bloquea.</li>
 * </ul>
 */
@Schema(description = "Clima actual en Huamanga")
public record ClimaResponse(

        @Schema(description = "Temperatura en grados Celsius", example = "17.4")
        Double temperatura,

        @Schema(description = "Sensacion termica en grados Celsius")
        Double sensacion,

        @Schema(description = "Humedad relativa en porcentaje")
        Integer humedad,

        @Schema(description = "Velocidad del viento en m/s")
        Double viento,

        /**
         * Codigo de condicion de OpenWeatherMap (Rain, Clouds, Clear...).
         *
         * <p>Se expone el codigo y no el texto ya traducido: el idioma lo elige
         * el navegador, y una descripcion armada en el servidor quedaria fija
         * en la primera lengua que se pidiera.</p>
         */
        @Schema(description = "Condicion meteorologica en codigo", example = "Rain")
        String condicion,

        @Schema(description = "Icono de OpenWeatherMap", example = "10d")
        String icono,

        @Schema(description = "Claves i18n de los consejos aplicables (RF-27)")
        List<String> consejos,

        @Schema(description = "Momento de la medicion")
        Instant medidoEn,

        @Schema(description = "False cuando no hay ningun dato que mostrar")
        boolean disponible,

        @Schema(description = "True cuando se sirve el ultimo dato conocido porque el proveedor no responde")
        boolean obsoleto
) {

    /** Respuesta cuando no hay nada cacheado y el proveedor esta caido. */
    public static ClimaResponse noDisponible() {
        return new ClimaResponse(null, null, null, null, null, null, List.of(), null, false, false);
    }

    /** El mismo dato, marcado como lo ultimo que se supo. */
    public ClimaResponse comoObsoleto() {
        return new ClimaResponse(temperatura, sensacion, humedad, viento, condicion,
                icono, consejos, medidoEn, true, true);
    }
}
