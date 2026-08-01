package com.huamanga.tourism.favorito.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** Clave primaria compuesta de {@link Favorito}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FavoritoId implements Serializable {

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "lugar_id", nullable = false)
    private UUID lugarId;

    public FavoritoId(UUID usuarioId, UUID lugarId) {
        this.usuarioId = usuarioId;
        this.lugarId = lugarId;
    }
}
