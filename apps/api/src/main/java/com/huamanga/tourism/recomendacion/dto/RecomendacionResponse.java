package com.huamanga.tourism.recomendacion.dto;

import com.huamanga.tourism.lugar.dto.LugarResumenResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Un lugar recomendado, con el porque (RF-08).
 *
 * @param motivos claves i18n de las razones que lo eligieron. Son claves y no
 *                frases por lo mismo que en los consejos del clima: el idioma
 *                lo decide el navegador. Ademas, exponer los motivos es lo que
 *                separa una recomendacion util de una caja negra: quien lee
 *                «esta abierto y a cubierto de la lluvia» entiende la sugerencia
 *                y puede discrepar con ella
 */
@Schema(description = "Lugar recomendado para el momento actual")
public record RecomendacionResponse(

        LugarResumenResponse lugar,

        @Schema(description = "Puntuacion interna, mayor es mejor", example = "45")
        int puntuacion,

        @Schema(description = "Claves i18n de los motivos", example = "[\"abiertoAhora\", \"aCubierto\"]")
        List<String> motivos
) {
}
