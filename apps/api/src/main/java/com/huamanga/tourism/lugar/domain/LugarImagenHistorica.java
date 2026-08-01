package com.huamanga.tourism.lugar.domain;

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
import org.locationtech.jts.geom.Point;

/**
 * Par de imagenes historica/actual de un lugar, para el slider antes/despues
 * (RF-11).
 *
 * <p>{@code puntoCaptura} guarda donde se tomo la foto historica y es lo que
 * habilita el modo geolocalizado "Parate aqui" (RF-11b): cuando el visitante
 * entra en un radio de 50 m de ese punto, la comparacion se abre a pantalla
 * completa desde el mismo angulo del original.</p>
 *
 * <p>Es GEOGRAPHY y no GEOMETRY a proposito: aqui se mide cercania real en
 * metros sobre la superficie terrestre, no distancia en un plano.</p>
 */
@Entity
@Table(name = "lugar_imagen_historica")
@Getter
@Setter
@NoArgsConstructor
public class LugarImagenHistorica extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id", nullable = false)
    private Lugar lugar;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "url_historica", nullable = false, length = 500)
    private String urlHistorica;

    @Column(name = "public_id_historica", nullable = false, length = 200)
    private String publicIdHistorica;

    /**
     * La BD limita el rango a 1500-1990 con un CHECK fijo. La regla dinamica
     * "al menos 50 anios de antiguedad" se valida en Bean Validation, porque
     * CURRENT_DATE no es inmutable y PostgreSQL no la admite en un CHECK.
     */
    @Column(name = "anio_historico", nullable = false)
    private Short anioHistorico;

    @Column(name = "url_actual", length = 500)
    private String urlActual;

    @Column(name = "public_id_actual", length = 200)
    private String publicIdActual;

    @Column(name = "credito_historico", length = 255)
    private String creditoHistorico;

    @Column(name = "punto_captura", columnDefinition = "geography(Point,4326)")
    private Point puntoCaptura;

    @Column(name = "orden", nullable = false)
    private Short orden = 0;
}
