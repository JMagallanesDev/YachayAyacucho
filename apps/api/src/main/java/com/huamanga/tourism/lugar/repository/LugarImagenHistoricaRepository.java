package com.huamanga.tourism.lugar.repository;

import com.huamanga.tourism.lugar.domain.LugarImagenHistorica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LugarImagenHistoricaRepository extends JpaRepository<LugarImagenHistorica, UUID> {

    List<LugarImagenHistorica> findByLugarIdOrderByOrdenAsc(UUID lugarId);

    /**
     * Las que se pueden usar en el modo «Parate aqui» (RF-11b).
     *
     * <p>Exige <strong>las dos cosas</strong>: punto de captura y foto actual.
     * Sin punto no se sabe donde hay que pararse, y sin foto actual no hay
     * comparacion que ensenar al llegar; ofrecer el modo en cualquiera de esos
     * dos casos seria invitar a alguien a caminar hasta un sitio para no
     * mostrarle nada.</p>
     */
    @Query("""
            SELECT i FROM LugarImagenHistorica i
            JOIN FETCH i.lugar
            WHERE i.puntoCaptura IS NOT NULL
              AND i.urlActual IS NOT NULL
            ORDER BY i.orden
            """)
    List<LugarImagenHistorica> findConPuntoDeCaptura();
}
