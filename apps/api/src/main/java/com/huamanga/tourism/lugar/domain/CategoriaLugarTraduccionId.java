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

/** Clave primaria compuesta de {@link CategoriaLugarTraduccion}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CategoriaLugarTraduccionId implements Serializable {

    @Column(name = "categoria_lugar_id", nullable = false)
    private UUID categoriaLugarId;

    @Column(name = "idioma", nullable = false, length = 5)
    private Idioma idioma;

    public CategoriaLugarTraduccionId(UUID categoriaLugarId, Idioma idioma) {
        this.categoriaLugarId = categoriaLugarId;
        this.idioma = idioma;
    }
}
