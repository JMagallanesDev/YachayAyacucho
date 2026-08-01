package com.huamanga.tourism.usuario.domain;

/** Estado de la cuenta de un usuario. */
public enum EstadoUsuario {

    ACTIVO,

    /** Bloqueado por moderacion; conserva sus datos. */
    SUSPENDIDO,

    /** Registrado pero aun sin verificar. */
    PENDIENTE
}
