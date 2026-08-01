package com.huamanga.tourism.ruta.domain;

import com.huamanga.tourism.lugar.domain.Lugar;
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

/**
 * Lugar dentro de una ruta tematica, con su orden de visita.
 *
 * <p>Pivote N:M con atributo propio: el {@code orden} pertenece a la relacion,
 * no al lugar ni a la ruta por separado (un mismo lugar puede ser el primero
 * de una ruta y el cuarto de otra).</p>
 *
 * <p>Es la tabla contra la que se cruza {@code check_in} para calcular el
 * progreso de una ruta sin almacenarlo (RF-39b).</p>
 */
@Entity
@Table(name = "lugar_ruta")
@Getter
@Setter
@NoArgsConstructor
public class LugarRuta {

    @EmbeddedId
    private LugarRutaId id;

    @MapsId("rutaTematicaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_tematica_id", nullable = false)
    private RutaTematica ruta;

    @MapsId("lugarId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id", nullable = false)
    private Lugar lugar;

    @Column(name = "orden", nullable = false)
    private Short orden;

    public LugarRuta(RutaTematica ruta, Lugar lugar, Short orden) {
        this.ruta = ruta;
        this.lugar = lugar;
        this.orden = orden;
        this.id = new LugarRutaId(ruta.getId(), lugar.getId());
    }
}
