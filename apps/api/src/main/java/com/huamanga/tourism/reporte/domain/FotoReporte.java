package com.huamanga.tourism.reporte.domain;

import com.huamanga.tourism.common.domain.EntidadCreacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Evidencia fotografica de un reporte ciudadano (hasta 5 por reporte). */
@Entity
@Table(name = "foto_reporte")
@Getter
@Setter
@NoArgsConstructor
public class FotoReporte extends EntidadCreacion {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id", nullable = false)
    private Reporte reporte;

    @Column(name = "cloudinary_url", nullable = false, length = 500)
    private String cloudinaryUrl;

    @Column(name = "cloudinary_public_id", nullable = false, length = 200)
    private String cloudinaryPublicId;

    @Column(name = "orden", nullable = false)
    private Short orden = 0;
}
