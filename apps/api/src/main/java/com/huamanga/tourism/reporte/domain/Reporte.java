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
import jakarta.persistence.PrePersist;
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
 *
 * <p><strong>El anonimato lo garantiza la propia entidad</strong>, no el
 * service que la guarde: vease {@link #borrarRastroSiEsAnonimo()}.</p>
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

    /**
     * Borra todo rastro de identidad cuando el reporte es anonimo (RF-72).
     *
     * <p><strong>Por que existe este metodo.</strong> Esta entidad hereda de
     * {@code EntidadAuditable}, que lleva {@code @CreatedBy} sobre
     * {@code created_by}, y el {@code AuditorAware} del proyecto rellena esa
     * columna con el usuario del SecurityContext. El resultado era una fuga
     * silenciosa: alguien <em>con sesion iniciada</em> que marcaba «anonimo»
     * obtenia un reporte con {@code usuario_id = NULL} y {@code es_anonimo =
     * true}... y su identificador escrito en {@code created_by}. El caso no es
     * raro, es el mas probable: quien usa la aplicacion, tiene cuenta y quiere
     * denunciar algo delicado sin que su nombre quede pegado.</p>
     *
     * <p><strong>Por que en la entidad y no en el service.</strong> Los
     * callbacks declarados en la clase se ejecutan <em>despues</em> de los
     * {@code EntityListener}, asi que este metodo anula justo lo que la
     * auditoria acaba de escribir. Y sobre todo: la garantia deja de depender
     * de que alguien se acuerde. Cualquier codigo que guarde un reporte
     * anonimo —hoy o dentro de tres bloques— la cumple sin saberlo.</p>
     *
     * <p>Solo en {@code @PrePersist}, no en {@code @PreUpdate}: al moderar,
     * {@code updated_by} identifica al <em>administrador</em> que tomo la
     * decision, no al denunciante, y esa trazabilidad si interesa conservarla.</p>
     */
    @PrePersist
    void borrarRastroSiEsAnonimo() {
        if (esAnonimo) {
            borrarAuditoria();
            // Tambien se anulan aqui y no solo en el service: si manana alguien
            // construye un Reporte desde otro sitio y olvida limpiarlos, la
            // entidad lo corrige igualmente.
            usuario = null;
            nombreReportante = null;
        }
    }
}
