package com.huamanga.tourism.negocio.domain;

/** Ciclo de aprobacion de un negocio del directorio (RF-104, RF-105). */
public enum EstadoNegocio {

    /** Recien registrado; no aparece en el directorio publico. */
    PENDIENTE,

    APROBADO,
    RECHAZADO,
    SUSPENDIDO
}
