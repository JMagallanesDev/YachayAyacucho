package com.huamanga.tourism.ruta.domain;

import com.huamanga.tourism.common.domain.EntidadAuditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ruta tematica que encadena varios lugares patrimoniales (RF-20, RF-53).
 *
 * <p>Se dibuja en el mapa como polilinea y su progreso alimenta el pasaporte
 * (RF-39b).</p>
 */
@Entity
@Table(name = "ruta_tematica")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class RutaTematica extends EntidadAuditable {

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(name = "icono", nullable = false, length = 50)
    private String icono;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @Column(name = "orden", nullable = false)
    private Short orden = 0;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RutaTraduccion> traducciones = new LinkedHashSet<>();

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LugarRuta> lugares = new LinkedHashSet<>();
}
