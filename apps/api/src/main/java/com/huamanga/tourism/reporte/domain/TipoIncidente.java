package com.huamanga.tourism.reporte.domain;

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

/** Uno de los 7 tipos de atentado al patrimonio predefinidos (RF-70). */
@Entity
@Table(name = "tipo_incidente")
@Getter
@Setter
@NoArgsConstructor
public class TipoIncidente extends EntidadBase {

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "icono", nullable = false, length = 50)
    private String icono;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @OneToMany(mappedBy = "tipoIncidente", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TipoIncidenteTraduccion> traducciones = new LinkedHashSet<>();
}
