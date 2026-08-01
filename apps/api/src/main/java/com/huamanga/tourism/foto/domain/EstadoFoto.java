package com.huamanga.tourism.foto.domain;

/**
 * Estado de moderacion de una foto subida por un usuario (RF-38, RF-49).
 *
 * <p>Nace {@code PENDIENTE}: la galeria publica solo muestra las aprobadas,
 * de modo que nada llega al sitio sin pasar por moderacion.</p>
 */
public enum EstadoFoto {

    PENDIENTE,
    APROBADA,
    RECHAZADA,
    EN_REVISION
}
