package com.huamanga.tourism.insignia.domain;

import com.huamanga.tourism.common.domain.Idioma;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** Clave primaria compuesta de {@link InsigniaTraduccion}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class InsigniaTraduccionId implements Serializable {

    @Column(name = "insignia_id", nullable = false)
    private UUID insigniaId;

    @Column(name = "idioma", nullable = false, length = 5)
    private Idioma idioma;

    public InsigniaTraduccionId(UUID insigniaId, Idioma idioma) {
        this.insigniaId = insigniaId;
        this.idioma = idioma;
    }
}
