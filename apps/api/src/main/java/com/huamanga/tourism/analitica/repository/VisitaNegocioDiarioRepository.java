package com.huamanga.tourism.analitica.repository;

import com.huamanga.tourism.analitica.domain.VisitaNegocioDiario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitaNegocioDiarioRepository extends JpaRepository<VisitaNegocioDiario, UUID> {

    Optional<VisitaNegocioDiario> findByNegocioIdAndFecha(UUID negocioId, LocalDate fecha);

    List<VisitaNegocioDiario> findByNegocioIdAndFechaBetweenOrderByFechaAsc(
            UUID negocioId, LocalDate desde, LocalDate hasta);
}
