package com.huamanga.tourism.negocio.domain;

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

/** Nombre traducido de una categoria de negocio. */
@Entity
@Table(name = "categoria_negocio_traduccion")
@Getter
@Setter
@NoArgsConstructor
public class CategoriaNegocioTraduccion extends MarcaTiempoTraduccion {

    @EmbeddedId
    private CategoriaNegocioTraduccionId id;

    @MapsId("categoriaNegocioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_negocio_id", nullable = false)
    private CategoriaNegocio categoria;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}
