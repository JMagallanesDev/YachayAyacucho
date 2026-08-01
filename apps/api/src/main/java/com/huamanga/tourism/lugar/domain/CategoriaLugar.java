package com.huamanga.tourism.lugar.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Categoria de lugar patrimonial (8 valores sembrados en la V14).
 *
 * <p>Separada de {@code CategoriaNegocio} a proposito: son entidades
 * semanticamente distintas con valores disjuntos, y unificarlas introduciria
 * una dependencia funcional debil que rompe la cohesion del dominio
 * (seccion 6.6).</p>
 */
@Entity
@Table(name = "categoria_lugar")
@Getter
@Setter
@NoArgsConstructor
public class CategoriaLugar extends EntidadBase {

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "icono", nullable = false, length = 50)
    private String icono;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(name = "orden", nullable = false)
    private Short orden;

    // Composicion real: una traduccion no existe sin su categoria, por eso
    // lleva cascade y orphanRemoval.
    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoriaLugarTraduccion> traducciones = new LinkedHashSet<>();
}
