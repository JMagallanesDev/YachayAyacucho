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

/**
 * Descripcion de un negocio en un idioma.
 *
 * <p>El espanol es obligatorio por regla de negocio; el ingles es opcional y
 * lo aporta el propio negocio desde su panel.</p>
 */
@Entity
@Table(name = "negocio_traduccion")
@Getter
@Setter
@NoArgsConstructor
public class NegocioTraduccion extends MarcaTiempoTraduccion {

    @EmbeddedId
    private NegocioTraduccionId id;

    @MapsId("negocioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;
}
