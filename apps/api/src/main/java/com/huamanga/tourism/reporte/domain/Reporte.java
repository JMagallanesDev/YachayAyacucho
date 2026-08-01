package com.huamanga.tourism.reporte.domain;

import com.huamanga.tourism.common.domain.EntidadAuditable;
import com.huamanga.tourism.usuario.domain.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.Point;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reporte ciudadano de un atentado al patrimonio. El diferenciador del
 * proyecto (Modulo 8).
 *
 * <p><strong>Esta entidad no almacena la IP del denunciante, ni siquiera
 * hasheada.</strong> Una IP hasheada es reversible por fuerza bruta: el
 * espacio IPv4 completo son ~4.300 millones de valores, que cualquier equipo
 * recorre en minutos. El anti-spam se resuelve con un contador volatil en
 * Redis con TTL de 24 h, que se autodestruye. Anonimato por diseno, no por
 * promesa (seccion 6.6).</p>
 *
 * <p>{@code usuario} es nulo en los reportes anonimos, y ese es el caso
 * esperado: denunciar no deberia exigir identificarse.</p>
 */
@Entity
@Table(name = "reporte")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Reporte extends EntidadAuditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_incidente_id", nullable = false)
    private TipoIncidente tipoIncidente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "nombre_reportante", length = 120)
    private String nombreReportante;

    @Column(name = "descripcion", nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "ubicacion", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point ubicacion;

    @Column(name = "direccion_referencial", length = 255)
    private String direccionReferencial;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReporte estado = EstadoReporte.RECIBIDO;

    /** Notas internas de moderacion; nunca se exponen al publico. */
    @Column(name = "notas_admin", columnDefinition = "text")
    private String notasAdmin;

    @Column(name = "es_anonimo", nullable = false)
    private boolean esAnonimo = true;

    @OneToMany(mappedBy = "reporte", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FotoReporte> fotos = new LinkedHashSet<>();
}
