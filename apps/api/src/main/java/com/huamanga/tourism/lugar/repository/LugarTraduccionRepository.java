package com.huamanga.tourism.lugar.repository;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.lugar.domain.LugarTraduccion;
import com.huamanga.tourism.lugar.domain.LugarTraduccionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LugarTraduccionRepository extends JpaRepository<LugarTraduccion, LugarTraduccionId> {

    List<LugarTraduccion> findByLugarId(UUID lugarId);

    Optional<LugarTraduccion> findByLugarIdAndIdIdioma(UUID lugarId, Idioma idioma);
}
