package com.huamanga.tourism.negocio.domain;

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
 * Categoria de negocio local (7 valores).
 *
 * <p>Independiente de {@code CategoriaLugar}: sus valores son disjuntos y
 * unirlas romperia la cohesion del dominio (seccion 6.6).</p>
 */
@Entity
@Table(name = "categoria_negocio")
@Getter
@Setter
@NoArgsConstructor
public class CategoriaNegocio extends EntidadBase {

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "icono", nullable = false, length = 50)
    private String icono;

    @Column(name = "orden", nullable = false)
    private Short orden;

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoriaNegocioTraduccion> traducciones = new LinkedHashSet<>();
}
