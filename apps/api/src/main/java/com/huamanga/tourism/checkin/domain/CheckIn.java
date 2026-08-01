package com.huamanga.tourism.checkin.domain;

import com.huamanga.tourism.common.domain.EntidadCreacion;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

/**
 * Visita verificada por GPS a un lugar patrimonial (RF-39).
 *
 * <p>Es un hecho inmutable: alimenta el pasaporte patrimonial (RF-39b) y el
 * contador de visitas de la vista materializada. El progreso por ruta NO se
 * guarda en ninguna parte: se calcula con un COUNT sobre check_in x
 * lugar_ruta, porque almacenarlo seria un atributo derivado y violaria 3FN
 * (seccion 6.6).</p>
 *
 * <p>Se conserva la posicion desde la que se hizo el check-in para poder
 * auditar despues que ocurrio realmente junto al lugar.</p>
 */
@Entity
@Table(name = "check_in")
@Getter
@Setter
@NoArgsConstructor
public class CheckIn extends EntidadCreacion {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id", nullable = false)
    private Lugar lugar;

    @Column(name = "ubicacion_gps", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point ubicacionGps;
}
