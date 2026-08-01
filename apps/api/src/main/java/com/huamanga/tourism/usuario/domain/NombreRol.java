package com.huamanga.tourism.usuario.domain;

/**
 * Los 4 roles del sistema (seccion 3 del plan).
 *
 * <p>Es un enum en Java y una fila en la tabla {@code rol} a la vez: el enum
 * da seguridad de tipos en {@code @PreAuthorize} y la tabla permite que el
 * rol sea una FK real con integridad referencial.</p>
 */
public enum NombreRol {

    /** Turista sin cuenta. */
    VISITANTE,

    /** Turista registrado: favoritos, resenas, fotos, check-in, pasaporte. */
    USUARIO,

    /** Dueno de negocio local con panel propio. */
    NEGOCIO,

    /** Gestor interno: CRUD, moderacion, metricas y auditoria. */
    ADMIN
}
