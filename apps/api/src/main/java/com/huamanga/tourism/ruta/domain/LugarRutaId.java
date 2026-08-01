package com.huamanga.tourism.ruta.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** Clave primaria compuesta de {@link LugarRuta}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LugarRutaId implements Serializable {

    @Column(name = "ruta_tematica_id", nullable = false)
    private UUID rutaTematicaId;

    @Column(name = "lugar_id", nullable = false)
    private UUID lugarId;

    public LugarRutaId(UUID rutaTematicaId, UUID lugarId) {
        this.rutaTematicaId = rutaTematicaId;
        this.lugarId = lugarId;
    }
}
