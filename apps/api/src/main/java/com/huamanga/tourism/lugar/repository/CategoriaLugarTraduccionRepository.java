package com.huamanga.tourism.lugar.repository;

import com.huamanga.tourism.lugar.domain.CategoriaLugarTraduccion;
import com.huamanga.tourism.lugar.domain.CategoriaLugarTraduccionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaLugarTraduccionRepository
        extends JpaRepository<CategoriaLugarTraduccion, CategoriaLugarTraduccionId> {
}
