package com.huamanga.tourism.lugar.repository;

import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de lugares patrimoniales.
 *
 * <p>Las consultas geoespaciales van en SQL nativo porque JPQL no conoce las
 * funciones ST_* de PostGIS. Se apoyan en el indice GIST
 * {@code idx_lugar_ubicacion}.</p>
 */
public interface LugarRepository extends JpaRepository<Lugar, UUID> {

    Optional<Lugar> findBySlug(String slug);

    /**
     * Ficha completa en una sola consulta.
     *
     * <p>Trae de golpe todo lo que necesita el detalle. Sin esto haria falta
     * una consulta por cada asociacion y, peor, con {@code open-in-view=false}
     * varias reventarian al construir la respuesta fuera de la transaccion.</p>
     *
     * <p>Se puede paginar sin riesgo porque devuelve un unico lugar: traer
     * colecciones en una consulta paginada obligaria a Hibernate a paginar en
     * memoria, algo que la configuracion tiene prohibido a proposito.</p>
     */
    @Query("""
            SELECT DISTINCT l FROM Lugar l
            JOIN FETCH l.categoria c
            LEFT JOIN FETCH c.traducciones
            JOIN FETCH l.distrito d
            JOIN FETCH d.provincia
            LEFT JOIN FETCH l.traducciones
            LEFT JOIN FETCH l.horarios
            WHERE l.slug = :slug
            """)
    Optional<Lugar> findBySlugConDetalle(String slug);

    /**
     * Listado paginado.
     *
     * <p>Solo se traen aqui las asociaciones a-uno. Las colecciones se cargan
     * despues en bloque gracias a {@code @BatchSize}: mezclar colecciones con
     * paginacion obligaria a Hibernate a traer todas las filas y paginar en
     * memoria.</p>
     */
    @Query(value = """
            SELECT l FROM Lugar l
            JOIN FETCH l.categoria
            JOIN FETCH l.distrito d
            JOIN FETCH d.provincia
            WHERE l.estado = :estado
            """,
            countQuery = "SELECT COUNT(l) FROM Lugar l WHERE l.estado = :estado")
    Page<Lugar> findByEstadoConDetalle(EstadoLugar estado, Pageable pageable);

    boolean existsBySlug(String slug);

    Page<Lugar> findByEstado(EstadoLugar estado, Pageable pageable);

    Page<Lugar> findByCategoriaIdAndEstado(UUID categoriaId, EstadoLugar estado, Pageable pageable);

    Page<Lugar> findByDistritoIdAndEstado(UUID distritoId, EstadoLugar estado, Pageable pageable);

    /**
     * Lugares publicados dentro de un radio en metros (RF-07).
     *
     * <p>Se convierte a {@code geography} para que la distancia se mida en
     * metros reales sobre la superficie terrestre y no en grados. ST_DWithin
     * sigue usando el indice GIST pese a la conversion, que es la razon de
     * usarla en lugar de calcular ST_Distance y filtrar despues.</p>
     */
    @Query(value = """
            SELECT * FROM lugar l
            WHERE l.deleted_at IS NULL
              AND l.estado = 'PUBLICADO'
              AND ST_DWithin(l.ubicacion::geography,
                             ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326)::geography,
                             :radioMetros)
            ORDER BY l.ubicacion::geography <-> ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326)::geography
            """, nativeQuery = true)
    List<Lugar> buscarCercaDe(@Param("longitud") double longitud,
                              @Param("latitud") double latitud,
                              @Param("radioMetros") double radioMetros);

    /**
     * Distancia en metros de un lugar a un punto, para el "a X min caminando"
     * (RF-09c).
     */
    @Query(value = """
            SELECT ST_Distance(l.ubicacion::geography,
                               ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326)::geography)
            FROM lugar l WHERE l.id = :lugarId
            """, nativeQuery = true)
    Double distanciaEnMetros(@Param("lugarId") UUID lugarId,
                             @Param("longitud") double longitud,
                             @Param("latitud") double latitud);

    /**
     * Busqueda de texto completo sobre las traducciones en espanol (RF-02).
     * Usa el indice GIN {@code idx_lugartrad_fulltext}.
     */
    @Query(value = """
            SELECT DISTINCT l.* FROM lugar l
            JOIN lugar_traduccion t ON t.lugar_id = l.id
            WHERE l.deleted_at IS NULL
              AND l.estado = 'PUBLICADO'
              AND t.idioma = 'es'
              AND to_tsvector('spanish',
                    COALESCE(t.nombre, '') || ' ' || COALESCE(t.descripcion, '') || ' ' || COALESCE(t.historia, ''))
                  @@ plainto_tsquery('spanish', :termino)
            """, nativeQuery = true)
    List<Lugar> buscarPorTexto(@Param("termino") String termino);
}
