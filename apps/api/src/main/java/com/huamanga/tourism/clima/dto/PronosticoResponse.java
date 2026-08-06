package com.huamanga.tourism.clima.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Pronostico por dias (RF-26).
 *
 * <p>El plan gratuito de OpenWeatherMap no entrega un pronostico diario: da
 * cinco dias en pasos de tres horas. Los dias de aqui se <strong>agregan</strong>
 * a partir de esos pasos (ver {@code ClimaService}). El pronostico diario de
 * ocho dias exige One Call, que es de pago por llamada.</p>
 */
@Schema(description = "Pronostico agregado por dias")
public record PronosticoResponse(

        List<PronosticoDia> dias,

        @Schema(description = "Momento en que se consulto al proveedor")
        Instant medidoEn,

        boolean disponible,

        boolean obsoleto
) {

    @Schema(description = "Resumen de un dia")
    public record PronosticoDia(

            LocalDate fecha,

            @Schema(description = "Temperatura minima en Celsius")
            Double minima,

            @Schema(description = "Temperatura maxima en Celsius")
            Double maxima,

            /** La condicion que mas se repite en las horas de visita del dia. */
            @Schema(description = "Condicion dominante", example = "Clouds")
            String condicion,

            String icono,

            @Schema(description = "Probabilidad de precipitacion, de 0 a 1")
            Double probabilidadLluvia,

            @Schema(description = "Claves i18n de los consejos del dia (RF-27)")
            List<String> consejos
    ) {
    }

    public static PronosticoResponse noDisponible() {
        return new PronosticoResponse(List.of(), null, false, false);
    }

    public PronosticoResponse comoObsoleto() {
        return new PronosticoResponse(dias, medidoEn, true, true);
    }
}
