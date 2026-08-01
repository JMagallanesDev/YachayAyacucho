package com.huamanga.tourism.resena.repository;

import com.huamanga.tourism.resena.domain.EstadoResena;
import com.huamanga.tourism.resena.domain.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResenaRepository extends JpaRepository<Resena, UUID> {

    Page<Resena> findByLugarIdAndEstado(UUID lugarId, EstadoResena estado, Pageable pageable);

    Optional<Resena> findByUsuarioIdAndLugarId(UUID usuarioId, UUID lugarId);

    /** Respalda en Java el UNIQUE (usuario, lugar) que impone la BD. */
    boolean existsByUsuarioIdAndLugarId(UUID usuarioId, UUID lugarId);

    long countByLugarIdAndEstado(UUID lugarId, EstadoResena estado);
}
