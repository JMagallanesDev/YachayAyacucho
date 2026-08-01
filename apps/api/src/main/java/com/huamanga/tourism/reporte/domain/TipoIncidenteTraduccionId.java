package com.huamanga.tourism.reporte.domain;

import com.huamanga.tourism.common.domain.Idioma;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** Clave primaria compuesta de {@link TipoIncidenteTraduccion}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class TipoIncidenteTraduccionId implements Serializable {

    @Column(name = "tipo_incidente_id", nullable = false)
    private UUID tipoIncidenteId;

    @Column(name = "idioma", nullable = false, length = 5)
    private Idioma idioma;

    public TipoIncidenteTraduccionId(UUID tipoIncidenteId, Idioma idioma) {
        this.tipoIncidenteId = tipoIncidenteId;
        this.idioma = idioma;
    }
}
