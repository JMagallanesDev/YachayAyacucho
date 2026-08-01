package com.huamanga.tourism.analitica.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Visitas agregadas por seccion y dia.
 *
 * <p>Es una tabla de hechos del patron data warehouse. Sus contadores no
 * derivan de ninguna otra tabla porque los eventos crudos de visita
 * <strong>no se persisten por diseno</strong>, por privacidad y por volumen:
 * esta tabla ES la fuente primaria del dato y sus atributos dependen
 * unicamente de su clave (dimension + fecha). Por eso cumple 3FN, aunque a
 * primera vista parezca un agregado (secciones 6.1 y 10.3).</p>
 */
@Entity
@Table(
        name = "visita_resumen_diario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_visita_resumen_dia",
                columnNames = {"tipo_pagina", "fecha"})
)
@Getter
@Setter
@NoArgsConstructor
public class VisitaResumenDiario extends EntidadBase {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagina", nullable = false, length = 30)
    private TipoPagina tipoPagina;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "total_visitas", nullable = false)
    private Integer totalVisitas = 0;

    @Column(name = "visitas_unicas", nullable = false)
    private Integer visitasUnicas = 0;
}
