package com.huamanga.tourism.resena.repository;

import com.huamanga.tourism.resena.domain.EstadoResena;
import com.huamanga.tourism.resena.domain.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResenaRepository extends JpaRepository<Resena, UUID> {

    Page<Resena> findByLugarIdAndEstado(UUID lugarId, EstadoResena estado, Pageable pageable);

    /**
     * Reseñas con su autor ya cargado.
     *
     * <p>La relacion con {@code Usuario} es perezosa, asi que pintar veinte
     * reseñas con el nombre de quien las escribio costaria veintiuna consultas.
     * El {@code JOIN FETCH} las convierte en una.</p>
     */
    @Query(value = """
            SELECT r FROM Resena r
            JOIN FETCH r.usuario
            WHERE r.lugar.id = :lugarId AND r.estado = :estado
            ORDER BY r.createdAt DESC
            """,
            countQuery = """
                    SELECT COUNT(r) FROM Resena r
                    WHERE r.lugar.id = :lugarId AND r.estado = :estado
                    """)
    Page<Resena> findByLugarIdAndEstadoConAutor(@Param("lugarId") UUID lugarId,
                                                @Param("estado") EstadoResena estado,
                                                Pageable pageable);

    /**
     * Bandeja de moderacion (RF-50).
     *
     * <p>Primero las editadas despues de publicarse y luego las mas recientes:
     * una reseña que cambio tras haber sido revisada es justo la que conviene
     * volver a mirar.</p>
     */
    @Query("""
            SELECT r FROM Resena r
            JOIN FETCH r.usuario
            JOIN FETCH r.lugar
            WHERE r.estado IN :estados
            ORDER BY r.updatedAt DESC
            """)
    List<Resena> paraModerar(@Param("estados") List<EstadoResena> estados, Pageable pageable);

    Optional<Resena> findByUsuarioIdAndLugarId(UUID usuarioId, UUID lugarId);

    /** Respalda en Java el UNIQUE (usuario, lugar) que impone la BD. */
    boolean existsByUsuarioIdAndLugarId(UUID usuarioId, UUID lugarId);

    long countByLugarIdAndEstado(UUID lugarId, EstadoResena estado);
}
