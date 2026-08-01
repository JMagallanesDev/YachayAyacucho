package com.huamanga.tourism.lugar.domain;

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

/**
 * Nombre traducido de una categoria de lugar.
 *
 * <p>{@code @MapsId} hace que la parte {@code categoriaLugarId} de la clave
 * compuesta y la FK sean la misma columna, sin duplicarla.</p>
 */
@Entity
@Table(name = "categoria_lugar_traduccion")
@Getter
@Setter
@NoArgsConstructor
public class CategoriaLugarTraduccion extends MarcaTiempoTraduccion {

    @EmbeddedId
    private CategoriaLugarTraduccionId id;

    @MapsId("categoriaLugarId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_lugar_id", nullable = false)
    private CategoriaLugar categoria;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}
