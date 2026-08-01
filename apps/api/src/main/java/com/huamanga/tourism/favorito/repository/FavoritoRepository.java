package com.huamanga.tourism.favorito.repository;

import com.huamanga.tourism.favorito.domain.Favorito;
import com.huamanga.tourism.favorito.domain.FavoritoId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface FavoritoRepository extends JpaRepository<Favorito, FavoritoId> {

    /** JOIN FETCH del lugar: la lista de favoritos siempre lo necesita. */
    @Query("""
            SELECT f FROM Favorito f
            JOIN FETCH f.lugar
            WHERE f.id.usuarioId = :usuarioId
            ORDER BY f.createdAt DESC
            """)
    Page<Favorito> findByUsuarioConLugar(UUID usuarioId, Pageable pageable);

    boolean existsByIdUsuarioIdAndIdLugarId(UUID usuarioId, UUID lugarId);

    long countByIdLugarId(UUID lugarId);
}
