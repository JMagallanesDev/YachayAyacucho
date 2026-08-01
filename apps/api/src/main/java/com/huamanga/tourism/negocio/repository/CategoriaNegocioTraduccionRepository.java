package com.huamanga.tourism.negocio.repository;

import com.huamanga.tourism.negocio.domain.CategoriaNegocioTraduccion;
import com.huamanga.tourism.negocio.domain.CategoriaNegocioTraduccionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaNegocioTraduccionRepository
        extends JpaRepository<CategoriaNegocioTraduccion, CategoriaNegocioTraduccionId> {
}
