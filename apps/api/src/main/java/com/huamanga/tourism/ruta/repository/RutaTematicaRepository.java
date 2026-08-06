package com.huamanga.tourism.ruta.repository;

import com.huamanga.tourism.ruta.domain.RutaTematica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RutaTematicaRepository extends JpaRepository<RutaTematica, UUID> {

    Optional<RutaTematica> findBySlug(String slug);

    List<RutaTematica> findByActivaTrueOrderByOrdenAsc();

    /**
     * Rutas con todo lo que hace falta para dibujarlas, en una sola consulta.
     *
     * <p>Sin estos {@code JOIN FETCH}, pintar tres rutas de cinco paradas
     * dispararia una consulta por ruta, otra por parada y otra por traduccion
     * de cada lugar. El {@code DISTINCT} es obligatorio: unir varias
     * colecciones multiplica las filas y la misma ruta volveria repetida.</p>
     */
    @Query("""
            SELECT DISTINCT r FROM RutaTematica r
            LEFT JOIN FETCH r.traducciones
            LEFT JOIN FETCH r.lugares enlace
            LEFT JOIN FETCH enlace.lugar lugar
            LEFT JOIN FETCH lugar.traducciones
            WHERE r.activa = true
            ORDER BY r.orden
            """)
    List<RutaTematica> findActivasConRecorrido();

    @Query("""
            SELECT DISTINCT r FROM RutaTematica r
            LEFT JOIN FETCH r.traducciones
            LEFT JOIN FETCH r.lugares enlace
            LEFT JOIN FETCH enlace.lugar lugar
            LEFT JOIN FETCH lugar.traducciones
            WHERE r.slug = :slug AND r.activa = true
            """)
    Optional<RutaTematica> findBySlugConRecorrido(@Param("slug") String slug);
}
