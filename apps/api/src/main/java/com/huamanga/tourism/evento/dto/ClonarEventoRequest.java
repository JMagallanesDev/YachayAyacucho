package com.huamanga.tourism.evento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Clonado anual de una festividad (RF-86).
 *
 * <p><strong>Por que las fechas son opcionales y el anio no.</strong> La Semana
 * Santa de Ayacucho —la festividad mas importante del calendario de esta
 * aplicacion— es movil: va atada a la Pascua y puede desplazarse casi un mes de
 * un anio a otro. Lo mismo el Carnaval. Un clonado que copiara la fecha vieja
 * produciria datos falsos justo en el evento que mas gente consulta.</p>
 *
 * <p>Asi que el anio es lo unico obligatorio y las fechas que propone el sistema
 * son <em>una sugerencia</em>: quien clona puede escribir las reales. Y en
 * cualquier caso el clon nace en BORRADOR, de modo que nadie ve unas fechas que
 * una persona no haya confirmado.</p>
 */
@Schema(description = "Anio de destino y, opcionalmente, las fechas reales")
public record ClonarEventoRequest(

        @Schema(example = "2027")
        @NotNull(message = "{evento.anio.obligatorio}")
        @Min(value = 2000, message = "{evento.anio.rango}")
        @Max(value = 2100, message = "{evento.anio.rango}")
        Integer anio,

        @Schema(description = "Fecha real de inicio. Si se omite, se desplaza la del original")
        LocalDate fechaInicio,

        @Schema(description = "Fecha real de fin. Si se omite, se conserva la duracion del original")
        LocalDate fechaFin
) {
}
