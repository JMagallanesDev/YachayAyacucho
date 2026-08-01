package com.huamanga.tourism.lugar.domain;

import com.huamanga.tourism.common.domain.EntidadAuditable;
import com.huamanga.tourism.geografia.domain.Distrito;
import com.huamanga.tourism.horario.domain.HorarioLugar;
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

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lugar patrimonial: el nucleo del sistema y del titulo de la tesis.
 *
 * <p>No tiene columna de horario ni de calificacion promedio, y es
 * deliberado:</p>
 * <ul>
 *   <li>El horario vive en {@link HorarioLugar}, en filas por dia y turno,
 *       porque un texto libre no es computable y sin el no existirian el
 *       "abierto ahora" (RF-09b), las recomendaciones (RF-08) ni el
 *       planificador (RF-29).</li>
 *   <li>La calificacion promedio se lee siempre de la vista materializada
 *       {@code estadistica_lugar}. Guardarla aqui seria un atributo derivado
 *       y violaria 3FN.</li>
 * </ul>
 *
 * <p>Tiene {@code distrito} y {@code ubicacion} a la vez sin que eso sea una
 * dependencia transitiva: son datos capturados por separado. Las coordenadas
 * son un punto exacto; el distrito es una asignacion administrativa que puede
 * decidirse a mano cuando el lugar cae en un limite distrital
 * (seccion 6.6).</p>
 */
@Entity
@Table(name = "lugar")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Lugar extends EntidadAuditable {

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_lugar_id", nullable = false)
    private CategoriaLugar categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distrito_id", nullable = false)
    private Distrito distrito;

    /** Punto en SRID 4326. La BD comprueba que caiga dentro de Ayacucho (RF-22b). */
    @Column(name = "ubicacion", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point ubicacion;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "telefono", length = 30)
    private String telefono;

    // ---- Bloque "Antes de ir" (RF-09d). Nullables porque se desconocen
    // ---- hasta que el administrador los verifica en campo.
    @Column(name = "precio_entrada_pen", precision = 8, scale = 2)
    private BigDecimal precioEntradaPen;

    @Column(name = "duracion_visita_min")
    private Short duracionVisitaMin;

    @Column(name = "acepta_tarjeta")
    private Boolean aceptaTarjeta;

    @Column(name = "tiene_banos")
    private Boolean tieneBanos;

    @Column(name = "accesible_silla_ruedas")
    private Boolean accesibleSillaRuedas;

    @Column(name = "apto_ninos")
    private Boolean aptoNinos;

    @Column(name = "costo_taxi_desde_plaza_pen", precision = 8, scale = 2)
    private BigDecimal costoTaxiDesdePlazaPen;

    @Column(name = "requiere_guia")
    private Boolean requiereGuia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoLugar estado = EstadoLugar.BORRADOR;

    // Composicion: horarios y traducciones no existen sin su lugar.
    @OneToMany(mappedBy = "lugar", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HorarioLugar> horarios = new LinkedHashSet<>();

    @OneToMany(mappedBy = "lugar", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LugarTraduccion> traducciones = new LinkedHashSet<>();

    @OneToMany(mappedBy = "lugar", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LugarImagenHistorica> imagenesHistoricas = new LinkedHashSet<>();

    public void agregarHorario(HorarioLugar horario) {
        horarios.add(horario);
        horario.setLugar(this);
    }

    public void agregarTraduccion(LugarTraduccion traduccion) {
        traducciones.add(traduccion);
        traduccion.setLugar(this);
    }
}
