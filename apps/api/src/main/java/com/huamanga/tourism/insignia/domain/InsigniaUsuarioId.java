package com.huamanga.tourism.insignia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** Clave primaria compuesta de {@link InsigniaUsuario}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class InsigniaUsuarioId implements Serializable {

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "insignia_id", nullable = false)
    private UUID insigniaId;

    public InsigniaUsuarioId(UUID usuarioId, UUID insigniaId) {
        this.usuarioId = usuarioId;
        this.insigniaId = insigniaId;
    }
}
