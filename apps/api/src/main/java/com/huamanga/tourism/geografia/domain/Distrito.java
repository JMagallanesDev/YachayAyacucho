package com.huamanga.tourism.geografia.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Distrito. La provincia se deriva del distrito, nunca al reves.
 */
@Entity
@Table(name = "distrito")
@Getter
@Setter
@NoArgsConstructor
public class Distrito extends EntidadBase {

    // LAZY explicito: el default de @ManyToOne es EAGER y es la causa mas
    // frecuente del problema N+1. Se corrige en todas las asociaciones.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provincia_id", nullable = false)
    private Provincia provincia;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}
