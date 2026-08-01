package com.huamanga.tourism.insignia.repository;

import com.huamanga.tourism.insignia.domain.InsigniaUsuario;
import com.huamanga.tourism.insignia.domain.InsigniaUsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InsigniaUsuarioRepository extends JpaRepository<InsigniaUsuario, InsigniaUsuarioId> {

    /** Insignias del pasaporte de un usuario, con su definicion cargada. */
    @Query("""
            SELECT iu FROM InsigniaUsuario iu
            JOIN FETCH iu.insignia
            WHERE iu.id.usuarioId = :usuarioId
            ORDER BY iu.obtenidaEn DESC
            """)
    List<InsigniaUsuario> findByUsuarioConInsignia(UUID usuarioId);

    boolean existsByIdUsuarioIdAndIdInsigniaId(UUID usuarioId, UUID insigniaId);

    long countByIdUsuarioId(UUID usuarioId);
}
