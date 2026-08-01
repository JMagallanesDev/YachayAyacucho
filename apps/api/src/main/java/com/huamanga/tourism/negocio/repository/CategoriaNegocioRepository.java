package com.huamanga.tourism.negocio.repository;

import com.huamanga.tourism.negocio.domain.CategoriaNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaNegocioRepository extends JpaRepository<CategoriaNegocio, UUID> {

    Optional<CategoriaNegocio> findByCodigo(String codigo);

    @Query("SELECT DISTINCT c FROM CategoriaNegocio c LEFT JOIN FETCH c.traducciones ORDER BY c.orden")
    List<CategoriaNegocio> findAllConTraducciones();
}
