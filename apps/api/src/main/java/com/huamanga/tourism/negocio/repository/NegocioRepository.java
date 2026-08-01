package com.huamanga.tourism.negocio.repository;

import com.huamanga.tourism.negocio.domain.EstadoNegocio;
import com.huamanga.tourism.negocio.domain.Negocio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NegocioRepository extends JpaRepository<Negocio, UUID> {

    /** Listado publico: solo los aprobados (RF-105). */
    Page<Negocio> findByEstado(EstadoNegocio estado, Pageable pageable);

    Page<Negocio> findByCategoriaIdAndEstado(UUID categoriaId, EstadoNegocio estado, Pageable pageable);

    List<Negocio> findByUsuarioId(UUID usuarioId);

    long countByEstado(EstadoNegocio estado);
}
