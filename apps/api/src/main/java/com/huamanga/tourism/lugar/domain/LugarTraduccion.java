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

import java.util.UUID;

/**
 * Contenido editorial de un lugar en un idioma.
 *
 * <p>Aqui vive el texto patrimonial que publica el administrador: nombre,
 * descripcion, historia y consejos. Es contenido de dominio, no de interfaz,
 * por eso esta en la base de datos y no en los archivos de traduccion del
 * frontend: es auditable, editable sin desplegar y traducible por registro
 * (seccion 5.5 del plan).</p>
 *
 * <p>Sobre {@code descripcion} e {@code historia} se construye el indice GIN
 * de busqueda de texto completo (RF-02).</p>
 */
@Entity
@Table(name = "lugar_traduccion")
@Getter
@Setter
@NoArgsConstructor
public class LugarTraduccion extends MarcaTiempoTraduccion {

    @EmbeddedId
    private LugarTraduccionId id;

    @MapsId("lugarId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id", nullable = false)
    private Lugar lugar;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "historia", columnDefinition = "text")
    private String historia;

    @Column(name = "consejos", columnDefinition = "text")
    private String consejos;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
