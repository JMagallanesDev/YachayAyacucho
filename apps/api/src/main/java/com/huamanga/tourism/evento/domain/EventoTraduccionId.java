package com.huamanga.tourism.evento.domain;

import com.huamanga.tourism.common.domain.Idioma;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** Clave primaria compuesta de {@link EventoTraduccion}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class EventoTraduccionId implements Serializable {

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(name = "idioma", nullable = false, length = 5)
    private Idioma idioma;

    public EventoTraduccionId(UUID eventoId, Idioma idioma) {
        this.eventoId = eventoId;
        this.idioma = idioma;
    }
}
