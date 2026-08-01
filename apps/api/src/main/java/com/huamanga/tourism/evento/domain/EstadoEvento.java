package com.huamanga.tourism.evento.domain;

/** Ciclo de vida de un evento de la agenda. */
public enum EstadoEvento {

    BORRADOR,
    PUBLICADO,

    /** Se anuncio y no se realizara; sigue visible con el aviso. */
    CANCELADO,

    ARCHIVADO
}
