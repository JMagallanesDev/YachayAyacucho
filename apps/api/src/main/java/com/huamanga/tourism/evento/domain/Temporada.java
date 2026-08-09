package com.huamanga.tourism.evento.domain;

import java.time.LocalDate;
import java.time.Month;

/**
 * Las dos temporadas del ano andino, para los eventos que quedan mas alla del
 * pronostico (RF-88).
 *
 * <p><strong>Esto no es un pronostico y no debe presentarse como tal.</strong>
 * Es climatologia: en la sierra sur del Peru las lluvias se concentran entre
 * noviembre y marzo, y de abril a octubre la estacion es seca. Decirle a quien
 * planea un viaje para diciembre que va en epoca de lluvias es informacion util
 * y cierta; decirle que "hara 18 grados" seria inventar.</p>
 *
 * <p>Por eso el enum es <strong>cualitativo</strong> y no lleva temperaturas.
 * Si en algun momento se quieren cifras, deben venir de SENAMHI y citarse; no
 * de una estimacion escrita aqui.</p>
 */
public enum Temporada {

    /** Noviembre a marzo: llueve, sobre todo por la tarde. */
    LLUVIAS,

    /** Abril a octubre: dias despejados y noches frias. */
    SECA;

    public static Temporada de(LocalDate fecha) {
        return de(fecha.getMonth());
    }

    public static Temporada de(Month mes) {
        return switch (mes) {
            case NOVEMBER, DECEMBER, JANUARY, FEBRUARY, MARCH -> LLUVIAS;
            default -> SECA;
        };
    }
}
