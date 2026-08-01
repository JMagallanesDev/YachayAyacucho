package com.huamanga.tourism.ruta.domain;

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

/** Nombre y descripcion de una ruta tematica en un idioma. */
@Entity
@Table(name = "ruta_traduccion")
@Getter
@Setter
@NoArgsConstructor
public class RutaTraduccion extends MarcaTiempoTraduccion {

    @EmbeddedId
    private RutaTraduccionId id;

    @MapsId("rutaTematicaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_tematica_id", nullable = false)
    private RutaTematica ruta;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;
}
