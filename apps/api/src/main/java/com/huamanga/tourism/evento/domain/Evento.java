package com.huamanga.tourism.evento.domain;

import com.huamanga.tourism.common.domain.EntidadAuditable;
import com.huamanga.tourism.geografia.domain.Distrito;
import com.huamanga.tourism.lugar.domain.Lugar;
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

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Evento de la agenda cultural (RF-79, RF-80).
 *
 * <p>El lugar es opcional: hay festividades de distrito que no ocurren en un
 * lugar patrimonial concreto. El distrito, en cambio, siempre existe.</p>
 *
 * <p>{@code recurrenteAnual} marca las festividades que se repiten cada anio
 * y habilita el clonado anual del panel de administracion (RF-86).</p>
 */
@Entity
@Table(name = "evento")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Evento extends EntidadAuditable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lugar_id")
    private Lugar lugar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distrito_id", nullable = false)
    private Distrito distrito;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoEvento tipo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "cloudinary_url_portada", length = 500)
    private String cloudinaryUrlPortada;

    @Column(name = "recurrente_anual", nullable = false)
    private boolean recurrenteAnual = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEvento estado = EstadoEvento.BORRADOR;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventoTraduccion> traducciones = new LinkedHashSet<>();

    /** Si el evento cae dentro de un rango de fechas de viaje (RF-84b). */
    public boolean ocurreEntre(LocalDate desde, LocalDate hasta) {
        return !fechaInicio.isAfter(hasta) && !fechaFin.isBefore(desde);
    }
}
