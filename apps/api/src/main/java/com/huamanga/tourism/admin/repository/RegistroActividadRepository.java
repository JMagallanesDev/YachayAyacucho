package com.huamanga.tourism.admin.repository;

import com.huamanga.tourism.admin.domain.RegistroActividad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistroActividadRepository extends JpaRepository<RegistroActividad, UUID> {

    /** Listado cronologico del panel admin (RF-56). Usa idx_actividad_created. */
    Page<RegistroActividad> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<RegistroActividad> findByEntidadAndEntidadIdOrderByCreatedAtDesc(String entidad, UUID entidadId);
}
