package com.huamanga.tourism.checkin.repository;

import com.huamanga.tourism.checkin.domain.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    /**
     * Sellos del pasaporte: lugares DISTINTOS visitados (RF-39b).
     *
     * <p>Distintos y no total de check-ins: un sello representa haber estado en
     * un sitio, no cuantas veces se ha vuelto.</p>
     */
    @Query("SELECT COUNT(DISTINCT c.lugar.id) FROM CheckIn c WHERE c.usuario.id = :usuarioId")
    long contarLugaresDistintos(@Param("usuarioId") UUID usuarioId);

    /** Igual, restringido a una categoria (insignias PEREGRINO, HISTORIADOR). */
    @Query("""
            SELECT COUNT(DISTINCT c.lugar.id) FROM CheckIn c
            WHERE c.usuario.id = :usuarioId AND c.lugar.categoria.codigo = :codigoCategoria
            """)
    long contarLugaresDistintosDeCategoria(@Param("usuarioId") UUID usuarioId,
                                           @Param("codigoCategoria") String codigoCategoria);

    /** Enfriamiento: ¿ya hubo un check-in aqui despues de este instante? */
    boolean existsByUsuarioIdAndLugarIdAndCreatedAtAfter(UUID usuarioId, UUID lugarId, Instant desde);

    /**
     * El check-in mas reciente, para detectar saltos imposibles.
     *
     * <p>Se trae el lugar por {@code JOIN FETCH} porque hara falta su ubicacion
     * inmediatamente despues; sin el seria una consulta adicional garantizada.</p>
     */
    @Query("""
            SELECT c FROM CheckIn c
            JOIN FETCH c.lugar
            WHERE c.usuario.id = :usuarioId
            ORDER BY c.createdAt DESC
            LIMIT 1
            """)
    Optional<CheckIn> ultimoDe(@Param("usuarioId") UUID usuarioId);

    /** Historial del pasaporte, con el lugar cargado para pintarlo. */
    @Query("""
            SELECT c FROM CheckIn c
            JOIN FETCH c.lugar l
            JOIN FETCH l.categoria
            WHERE c.usuario.id = :usuarioId
            ORDER BY c.createdAt DESC
            """)
    List<CheckIn> historialDe(@Param("usuarioId") UUID usuarioId);
}
