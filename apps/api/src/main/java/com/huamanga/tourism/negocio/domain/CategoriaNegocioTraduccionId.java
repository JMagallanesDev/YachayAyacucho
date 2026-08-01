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

/** Clave primaria compuesta de {@link CategoriaNegocioTraduccion}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CategoriaNegocioTraduccionId implements Serializable {

    @Column(name = "categoria_negocio_id", nullable = false)
    private UUID categoriaNegocioId;

    @Column(name = "idioma", nullable = false, length = 5)
    private Idioma idioma;

    public CategoriaNegocioTraduccionId(UUID categoriaNegocioId, Idioma idioma) {
        this.categoriaNegocioId = categoriaNegocioId;
        this.idioma = idioma;
    }
}
