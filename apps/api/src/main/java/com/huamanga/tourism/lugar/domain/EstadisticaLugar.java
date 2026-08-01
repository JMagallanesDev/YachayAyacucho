package com.huamanga.tourism.lugar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Agregados por lugar: calificacion promedio, resenas, visitas y favoritos.
 *
 * <p><strong>No es una de las 35 entidades del modelo.</strong> Mapea la vista
 * materializada {@code estadistica_lugar}, que es el objeto numero 36 del
 * esquema. Se cuenta aparte a proposito: el plan declara "35 entidades + 1
 * vista materializada".</p>
 *
 * <p>Existe para no violar 3FN. Guardar la calificacion promedio como columna
 * de {@code lugar} seria una dependencia transitiva —el promedio depende de
 * las resenas, no del lugar— y obligaria a mantenerla sincronizada con
 * triggers. La vista es un objeto independiente que se recalcula, tecnica
 * reconocida en la literatura (Stonebraker, Date).</p>
 *
 * <p>Se mapea con {@code @Subselect} y no como tabla porque una vista
 * materializada no aparece en los metadatos JDBC como tabla, y la validacion
 * de esquema de Hibernate la daria por inexistente.</p>
 *
 * <p>{@code @Synchronize} le dice a Hibernate que esta vista depende de esas
 * tres tablas, para que vacie los cambios pendientes antes de consultarla.</p>
 */
@Entity
@Immutable
@Subselect("SELECT * FROM estadistica_lugar")
@Synchronize({"resena", "check_in", "favorito"})
@Getter
@NoArgsConstructor
public class EstadisticaLugar {

    @Id
    @Column(name = "lugar_id")
    private UUID lugarId;

    @Column(name = "calificacion_promedio")
    private BigDecimal calificacionPromedio;

    @Column(name = "total_resenas")
    private Long totalResenas;

    @Column(name = "total_visitas")
    private Long totalVisitas;

    @Column(name = "total_favoritos")
    private Long totalFavoritos;

    /** Momento del ultimo REFRESH, util para avisar de datos antiguos. */
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;
}
