package com.huamanga.tourism.lugar.domain;

/** Ciclo de vida editorial de un lugar patrimonial. */
public enum EstadoLugar {

    /** En edicion por el admin; no visible al publico. */
    BORRADOR,

    /** Visible en el sitio publico. */
    PUBLICADO,

    /** Retirado de la vista sin eliminarlo. */
    ARCHIVADO
}
