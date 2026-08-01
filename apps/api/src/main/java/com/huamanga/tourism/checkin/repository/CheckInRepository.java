package com.huamanga.tourism.checkin.repository;

import com.huamanga.tourism.checkin.domain.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    List<CheckIn> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);

    boolean existsByUsuarioIdAndLugarId(UUID usuarioId, UUID lugarId);

    long countByUsuarioId(UUID usuarioId);

    /**
     * Progreso de un usuario en una ruta tematica (RF-39b).
     *
     * <p>El progreso NO se almacena en ninguna tabla: seria un atributo
     * derivado y violaria 3FN. Se calcula aqui, cruzando los check-ins del
     * usuario con los lugares de la ruta.</p>
     */
    @Query("""
            SELECT COUNT(DISTINCT c.lugar.id) FROM CheckIn c
            WHERE c.usuario.id = :usuarioId
              AND c.lugar.id IN (SELECT lr.lugar.id FROM LugarRuta lr WHERE lr.ruta.id = :rutaId)
            """)
    long contarLugaresVisitadosDeRuta(UUID usuarioId, UUID rutaId);
}
