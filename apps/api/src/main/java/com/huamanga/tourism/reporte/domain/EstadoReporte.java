package com.huamanga.tourism.reporte.domain;

/** Ciclo de moderacion de un reporte ciudadano (RF-76). */
public enum EstadoReporte {

    /** Recien enviado por el ciudadano. */
    RECIBIDO,

    EN_REVISION,

    /** Validado: aparece en el mapa publico de incidentes (RF-74). */
    APROBADO,

    /** No procede; no se publica. */
    DESCARTADO,

    /** El atentado fue atendido. */
    RESUELTO
}
