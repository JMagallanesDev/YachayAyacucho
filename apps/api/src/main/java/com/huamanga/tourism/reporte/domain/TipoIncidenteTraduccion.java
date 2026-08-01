package com.huamanga.tourism.reporte.domain;

import com.huamanga.tourism.common.domain.MarcaTiempoTraduccion;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nombre traducido de un tipo de incidente. */
@Entity
@Table(name = "tipo_incidente_traduccion")
@Getter
@Setter
@NoArgsConstructor
public class TipoIncidenteTraduccion extends MarcaTiempoTraduccion {

    @EmbeddedId
    private TipoIncidenteTraduccionId id;

    @MapsId("tipoIncidenteId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_incidente_id", nullable = false)
    private TipoIncidente tipoIncidente;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}
