package com.huamanga.tourism.negocio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * El negocio visto por su propio dueno (RF-107).
 *
 * <p>Anade a la ficha publica tres cosas que solo le importan a el: el RUC que
 * declaro, el motivo si se le rechazo, y <strong>sus metricas</strong>.</p>
 *
 * <p>Las metricas son <strong>solo agregados diarios</strong>: cuantas visitas y
 * cuantos clics, nunca de quien. Es la misma linea del Bloque 10 y del anonimato
 * del Bloque 8 — el dueno de un restaurante no tiene por que saber quien miro su
 * ficha, solo cuanta gente lo hizo.</p>
 */
@Schema(description = "Negocio propio, con RUC y metricas agregadas")
public record MiNegocioResponse(

        NegocioResponse negocio,

        @Schema(description = "Visible solo para su dueno y para el administrador")
        String ruc,

        @Schema(description = "Por que se rechazo, si se rechazo")
        String motivoRechazo,

        @Schema(description = "Ultimos 30 dias, sin ningun dato de quien visito")
        List<DiaDeMetricas> metricas,

        Resumen resumen
) {

    @Schema(description = "Un dia de metricas del negocio")
    public record DiaDeMetricas(
            LocalDate fecha,
            long visitas,
            long clicsWhatsapp,
            long clicsComoLlegar
    ) {
    }

    @Schema(description = "Totales acumulados")
    public record Resumen(long visitas, long clicsWhatsapp, long clicsComoLlegar) {
    }
}
