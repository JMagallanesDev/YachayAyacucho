package com.huamanga.tourism.foto.repository;

import com.huamanga.tourism.foto.domain.EstadoFoto;
import com.huamanga.tourism.foto.domain.Foto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FotoRepository extends JpaRepository<Foto, UUID> {

    /** Bandeja de moderacion (RF-49). Usa el indice parcial idx_foto_pendientes. */
    Page<Foto> findByEstadoOrderByCreatedAtAsc(EstadoFoto estado, Pageable pageable);

    List<Foto> findByLugarIdAndEstado(UUID lugarId, EstadoFoto estado);

    long countByUsuarioIdAndLugarId(UUID usuarioId, UUID lugarId);
}
