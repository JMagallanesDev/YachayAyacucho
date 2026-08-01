package com.huamanga.tourism.geografia.repository;

import com.huamanga.tourism.geografia.domain.Distrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DistritoRepository extends JpaRepository<Distrito, UUID> {

    Optional<Distrito> findByCodigo(String codigo);

    List<Distrito> findByProvinciaIdOrderByNombreAsc(UUID provinciaId);

    /**
     * JOIN FETCH para traer distrito y provincia en una sola consulta. Sin el,
     * listar 119 distritos y leer el nombre de su provincia dispararia 119
     * consultas adicionales (problema N+1).
     */
    @Query("SELECT d FROM Distrito d JOIN FETCH d.provincia ORDER BY d.nombre")
    List<Distrito> findAllConProvincia();
}
