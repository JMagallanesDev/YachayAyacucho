package com.huamanga.tourism.negocio.domain;

import com.huamanga.tourism.common.domain.Idioma;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** Clave primaria compuesta de {@link NegocioTraduccion}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class NegocioTraduccionId implements Serializable {

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(name = "idioma", nullable = false, length = 5)
    private Idioma idioma;

    public NegocioTraduccionId(UUID negocioId, Idioma idioma) {
        this.negocioId = negocioId;
        this.idioma = idioma;
    }
}
