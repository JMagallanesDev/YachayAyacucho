package com.huamanga.tourism.evento.repository;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.evento.domain.EventoTraduccion;
import com.huamanga.tourism.evento.domain.EventoTraduccionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventoTraduccionRepository extends JpaRepository<EventoTraduccion, EventoTraduccionId> {

    List<EventoTraduccion> findByEventoId(UUID eventoId);

    Optional<EventoTraduccion> findByEventoIdAndIdIdioma(UUID eventoId, Idioma idioma);
}
