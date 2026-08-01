package com.huamanga.tourism.lugar.repository;

import com.huamanga.tourism.lugar.domain.LugarImagenHistorica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LugarImagenHistoricaRepository extends JpaRepository<LugarImagenHistorica, UUID> {

    List<LugarImagenHistorica> findByLugarIdOrderByOrdenAsc(UUID lugarId);
}
