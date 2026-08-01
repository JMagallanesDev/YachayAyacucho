package com.huamanga.tourism.geografia.domain;

import com.huamanga.tourism.common.domain.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Provincia de la region Ayacucho (11 en total).
 *
 * <p>Existe desde el inicio aunque el contenido sea solo de Huamanga: escalar
 * a toda la region sera insertar filas, no un ALTER TABLE arriesgado sobre
 * una base ya en produccion (seccion 6.6 del plan).</p>
 *
 * <p>Sin tabla de traduccion: los nombres son oficiales y no se traducen.</p>
 */
@Entity
@Table(name = "provincia")
@Getter
@Setter
@NoArgsConstructor
public class Provincia extends EntidadBase {

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private Short orden;
}
