package com.huamanga.tourism.foto.repository;

import com.huamanga.tourism.foto.domain.EstadoFoto;
import com.huamanga.tourism.foto.domain.Foto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FotoRepository extends JpaRepository<Foto, UUID> {

    /** Bandeja de moderacion (RF-49). Usa el indice parcial idx_foto_pendientes. */
    Page<Foto> findByEstadoOrderByCreatedAtAsc(EstadoFoto estado, Pageable pageable);

    List<Foto> findByLugarIdAndEstado(UUID lugarId, EstadoFoto estado);

    long countByUsuarioIdAndLugarId(UUID usuarioId, UUID lugarId);

    /** Insignia FOTOGRAFO: fotos aprobadas de una persona (RF-39b). */
    long countByUsuarioIdAndEstado(UUID usuarioId, EstadoFoto estado);

    /**
     * Galeria publica de un lugar, con el autor ya cargado.
     *
     * <p>Sin el {@code JOIN FETCH}, mostrar quien subio cada foto costaria una
     * consulta por foto. Las mas recientes primero: una galeria que empieza por
     * lo ultimo que subio la gente se siente viva.</p>
     */
    @Query("""
            SELECT f FROM Foto f
            JOIN FETCH f.usuario
            WHERE f.lugar.id = :lugarId AND f.estado = :estado
            ORDER BY f.createdAt DESC
            """)
    List<Foto> findByLugarIdAndEstadoConAutor(@Param("lugarId") UUID lugarId,
                                              @Param("estado") EstadoFoto estado);

    @Query("""
            SELECT f FROM Foto f
            JOIN FETCH f.usuario
            WHERE f.usuario.id = :usuarioId AND f.lugar.id = :lugarId
            ORDER BY f.createdAt DESC
            """)
    List<Foto> findByUsuarioIdAndLugarIdConAutor(@Param("usuarioId") UUID usuarioId,
                                                 @Param("lugarId") UUID lugarId);

    /** Bandeja de moderacion con lugar y autor, para no disparar N+1 (RF-49). */
    @Query("""
            SELECT f FROM Foto f
            JOIN FETCH f.usuario
            JOIN FETCH f.lugar
            WHERE f.estado = :estado
            ORDER BY f.createdAt ASC
            """)
    List<Foto> pendientesConDetalle(@Param("estado") EstadoFoto estado, Pageable pageable);
}
