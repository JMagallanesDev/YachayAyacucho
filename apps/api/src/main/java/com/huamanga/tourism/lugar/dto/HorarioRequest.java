package com.huamanga.tourism.lugar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Una franja horaria de un dia concreto.
 *
 * <p>Se envian varias por dia cuando el lugar cierra al mediodia, que es lo
 * habitual en los templos de Huamanga.</p>
 */
@Schema(description = "Franja horaria de un dia")
public record HorarioRequest(

        @Schema(description = "0 = domingo ... 6 = sabado", example = "1")
        @NotNull(message = "{horario.dia.obligatorio}")
        @Min(value = 0, message = "{horario.dia.rango}")
        @Max(value = 6, message = "{horario.dia.rango}")
        Short diaSemana,

        @Schema(example = "09:00")
        LocalTime horaApertura,

        @Schema(example = "13:00")
        LocalTime horaCierre,

        @Schema(description = "Si es true, el lugar no abre ese dia y las horas se ignoran")
        boolean cerrado
) {

    /** Un dia cerrado no necesita horas; uno abierto las necesita coherentes. */
    public boolean esCoherente() {
        if (cerrado) {
            return true;
        }
        return horaApertura != null && horaCierre != null && horaApertura.isBefore(horaCierre);
    }

    /** Si esta franja se solapa con otra del mismo dia. */
    public boolean seSolapaCon(HorarioRequest otra) {
        if (cerrado || otra.cerrado() || !diaSemana.equals(otra.diaSemana())) {
            return false;
        }
        if (!esCoherente() || !otra.esCoherente()) {
            return false;
        }
        // Dos intervalos se solapan si cada uno empieza antes de que acabe el
        // otro. Se toma el cierre como excluyente, de modo que 09:00-13:00 y
        // 13:00-18:00 son contiguos, no solapados.
        return horaApertura.isBefore(otra.horaCierre()) && otra.horaApertura().isBefore(horaCierre);
    }
}
