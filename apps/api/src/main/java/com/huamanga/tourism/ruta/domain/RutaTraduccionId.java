package com.huamanga.tourism.ruta.domain;

import com.huamanga.tourism.common.domain.Idioma;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** Clave primaria compuesta de {@link RutaTraduccion}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class RutaTraduccionId implements Serializable {

    @Column(name = "ruta_tematica_id", nullable = false)
    private UUID rutaTematicaId;

    @Column(name = "idioma", nullable = false, length = 5)
    private Idioma idioma;

    public RutaTraduccionId(UUID rutaTematicaId, Idioma idioma) {
        this.rutaTematicaId = rutaTematicaId;
        this.idioma = idioma;
    }
}
