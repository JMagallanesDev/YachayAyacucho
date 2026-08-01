package com.huamanga.tourism.analitica.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import com.huamanga.tourism.negocio.domain.Negocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Trafico diario de un negocio del directorio (RF-52b).
 *
 * <p>El UNIQUE (negocio, fecha) es lo que permite resolverlo con un UPSERT:
 * una sola fila por negocio y dia, incrementada con throttling desde el
 * frontend en lugar de una fila por clic.</p>
 */
@Entity
@Table(
        name = "visita_negocio_diario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_visita_negocio_dia",
                columnNames = {"negocio_id", "fecha"})
)
@Getter
@Setter
@NoArgsConstructor
public class VisitaNegocioDiario extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "total_visitas", nullable = false)
    private Integer totalVisitas = 0;

    @Column(name = "clics_whatsapp", nullable = false)
    private Integer clicsWhatsapp = 0;

    @Column(name = "clics_como_llegar", nullable = false)
    private Integer clicsComoLlegar = 0;
}
