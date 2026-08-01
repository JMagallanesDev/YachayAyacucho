package com.huamanga.tourism.lugar.domain;

import com.huamanga.tourism.common.domain.Idioma;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** Clave primaria compuesta de {@link LugarTraduccion}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LugarTraduccionId implements Serializable {

    @Column(name = "lugar_id", nullable = false)
    private UUID lugarId;

    @Column(name = "idioma", nullable = false, length = 5)
    private Idioma idioma;

    public LugarTraduccionId(UUID lugarId, Idioma idioma) {
        this.lugarId = lugarId;
        this.idioma = idioma;
    }
}
