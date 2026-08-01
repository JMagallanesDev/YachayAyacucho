package com.huamanga.tourism.negocio.repository;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.negocio.domain.NegocioTraduccion;
import com.huamanga.tourism.negocio.domain.NegocioTraduccionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NegocioTraduccionRepository extends JpaRepository<NegocioTraduccion, NegocioTraduccionId> {

    Optional<NegocioTraduccion> findByNegocioIdAndIdIdioma(UUID negocioId, Idioma idioma);
}
