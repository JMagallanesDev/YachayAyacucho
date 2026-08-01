package com.huamanga.tourism.ruta.repository;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.ruta.domain.RutaTraduccion;
import com.huamanga.tourism.ruta.domain.RutaTraduccionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RutaTraduccionRepository extends JpaRepository<RutaTraduccion, RutaTraduccionId> {

    Optional<RutaTraduccion> findByRutaIdAndIdIdioma(UUID rutaId, Idioma idioma);
}
