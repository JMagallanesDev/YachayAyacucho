package com.huamanga.tourism.ruta.repository;

import com.huamanga.tourism.ruta.domain.RutaTematica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RutaTematicaRepository extends JpaRepository<RutaTematica, UUID> {

    Optional<RutaTematica> findBySlug(String slug);

    List<RutaTematica> findByActivaTrueOrderByOrdenAsc();
}
