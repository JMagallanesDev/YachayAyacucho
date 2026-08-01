package com.huamanga.tourism.lugar.repository;

import com.huamanga.tourism.lugar.domain.CategoriaLugar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaLugarRepository extends JpaRepository<CategoriaLugar, UUID> {

    Optional<CategoriaLugar> findByCodigo(String codigo);

    /** Categorias con sus traducciones en una sola consulta (evita N+1). */
    @Query("SELECT DISTINCT c FROM CategoriaLugar c LEFT JOIN FETCH c.traducciones ORDER BY c.orden")
    List<CategoriaLugar> findAllConTraducciones();
}
