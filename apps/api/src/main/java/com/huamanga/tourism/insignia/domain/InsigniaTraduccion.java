package com.huamanga.tourism.insignia.domain;

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

/** Nombre y descripcion de una insignia en un idioma. */
@Entity
@Table(name = "insignia_traduccion")
@Getter
@Setter
@NoArgsConstructor
public class InsigniaTraduccion extends MarcaTiempoTraduccion {

    @EmbeddedId
    private InsigniaTraduccionId id;

    @MapsId("insigniaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insignia_id", nullable = false)
    private Insignia insignia;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 300)
    private String descripcion;
}
