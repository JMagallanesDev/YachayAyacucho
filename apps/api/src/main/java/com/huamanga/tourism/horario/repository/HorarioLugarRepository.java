package com.huamanga.tourism.horario.repository;

import com.huamanga.tourism.horario.domain.HorarioLugar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Horarios de lugares patrimoniales.
 *
 * <p>La consulta por lugar y dia es la que sostiene el badge
 * "abierto/cerrado ahora" (RF-09b) y se apoya en el indice compuesto
 * {@code idx_horario_lugar_dia}.</p>
 */
public interface HorarioLugarRepository extends JpaRepository<HorarioLugar, UUID> {

    List<HorarioLugar> findByLugarIdAndDiaSemana(UUID lugarId, Short diaSemana);

    List<HorarioLugar> findByLugarIdOrderByDiaSemanaAsc(UUID lugarId);

    void deleteByLugarId(UUID lugarId);
}
