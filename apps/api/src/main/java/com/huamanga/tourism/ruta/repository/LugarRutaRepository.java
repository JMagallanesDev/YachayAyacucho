package com.huamanga.tourism.ruta.repository;

import com.huamanga.tourism.ruta.domain.LugarRuta;
import com.huamanga.tourism.ruta.domain.LugarRutaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LugarRutaRepository extends JpaRepository<LugarRuta, LugarRutaId> {

    /** Lugares de una ruta en su orden de visita, con el lugar ya cargado. */
    @Query("""
            SELECT lr FROM LugarRuta lr
            JOIN FETCH lr.lugar
            WHERE lr.ruta.id = :rutaId
            ORDER BY lr.orden
            """)
    List<LugarRuta> findByRutaConLugar(UUID rutaId);

    long countByRutaId(UUID rutaId);

    List<LugarRuta> findByLugarId(UUID lugarId);
}
