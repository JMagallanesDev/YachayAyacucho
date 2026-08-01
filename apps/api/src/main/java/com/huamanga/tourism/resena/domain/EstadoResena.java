package com.huamanga.tourism.resena.domain;

/**
 * Estado de moderacion de una resena.
 *
 * <p>Solo las {@code PUBLICADA} cuentan para la calificacion promedio de la
 * vista materializada: una resena oculta no debe mover el promedio publico.</p>
 */
public enum EstadoResena {

    PUBLICADA,
    OCULTA,
    ELIMINADA,

    /** Alcanzo 3 reportes de contenido y espera revision (RF-45). */
    EN_REVISION
}
