package com.huamanga.tourism.analitica.repository;

import com.huamanga.tourism.analitica.domain.TipoPagina;
import com.huamanga.tourism.analitica.domain.VisitaResumenDiario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitaResumenDiarioRepository extends JpaRepository<VisitaResumenDiario, UUID> {

    Optional<VisitaResumenDiario> findByTipoPaginaAndFecha(TipoPagina tipoPagina, LocalDate fecha);

    List<VisitaResumenDiario> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}
