package com.huamanga.tourism.negocio.repository;

import com.huamanga.tourism.negocio.domain.EstadoNegocio;
import com.huamanga.tourism.negocio.domain.Negocio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NegocioRepository extends JpaRepository<Negocio, UUID> {

    /** Listado publico: solo los aprobados (RF-105). */
    Page<Negocio> findByEstado(EstadoNegocio estado, Pageable pageable);

    Page<Negocio> findByCategoriaIdAndEstado(UUID categoriaId, EstadoNegocio estado, Pageable pageable);

    List<Negocio> findByUsuarioId(UUID usuarioId);

    long countByEstado(EstadoNegocio estado);

    /**
     * Directorio publico con todo lo que pinta una tarjeta, en una consulta.
     *
     * <p>El estado <strong>va escrito en la consulta</strong>, no llega por
     * parametro: asi no existe forma de pedir por aqui un negocio pendiente,
     * ni por descuido ni manipulando la peticion.</p>
     *
     * <p>Las traducciones del negocio no se traen con FETCH: unir dos
     * colecciones —las suyas y las de su categoria— multiplicaria las filas y
     * obligaria a Hibernate a paginar en memoria, que es justo lo que rompe un
     * listado paginado. Se dejan perezosas y se resuelven dentro de la
     * transaccion, que sigue abierta al mapear.</p>
     */
    @Query(value = """
            SELECT n FROM Negocio n
            JOIN FETCH n.categoria c
            LEFT JOIN FETCH c.traducciones
            JOIN FETCH n.distrito
            WHERE n.estado = com.huamanga.tourism.negocio.domain.EstadoNegocio.APROBADO
            ORDER BY n.nombre
            """,
            countQuery = """
            SELECT COUNT(n) FROM Negocio n
            WHERE n.estado = com.huamanga.tourism.negocio.domain.EstadoNegocio.APROBADO
            """)
    Page<Negocio> findAprobadosConDetalle(Pageable pagina);

    @Query(value = """
            SELECT n FROM Negocio n
            JOIN FETCH n.categoria c
            LEFT JOIN FETCH c.traducciones
            JOIN FETCH n.distrito
            WHERE n.estado = com.huamanga.tourism.negocio.domain.EstadoNegocio.APROBADO
              AND c.id = :categoriaId
            ORDER BY n.nombre
            """,
            countQuery = """
            SELECT COUNT(n) FROM Negocio n
            WHERE n.estado = com.huamanga.tourism.negocio.domain.EstadoNegocio.APROBADO
              AND n.categoria.id = :categoriaId
            """)
    Page<Negocio> findAprobadosPorCategoria(@Param("categoriaId") UUID categoriaId, Pageable pagina);

    /** Ficha publica: tambien filtra por APROBADO dentro de la consulta. */
    @Query("""
            SELECT n FROM Negocio n
            JOIN FETCH n.categoria c
            LEFT JOIN FETCH c.traducciones
            JOIN FETCH n.distrito
            LEFT JOIN FETCH n.traducciones
            WHERE n.id = :id
              AND n.estado = com.huamanga.tourism.negocio.domain.EstadoNegocio.APROBADO
            """)
    Optional<Negocio> findAprobadoConDetalle(@Param("id") UUID id);

    /** Bandeja del administrador: todos los estados, los pendientes primero. */
    @Query("""
            SELECT n FROM Negocio n
            JOIN FETCH n.categoria c
            LEFT JOIN FETCH c.traducciones
            JOIN FETCH n.distrito
            JOIN FETCH n.usuario
            ORDER BY CASE WHEN n.estado = com.huamanga.tourism.negocio.domain.EstadoNegocio.PENDIENTE
                          THEN 0 ELSE 1 END, n.createdAt DESC
            """)
    List<Negocio> findTodosParaModerar();

    /** Los negocios de una persona, en cualquier estado (RF-107). */
    @Query("""
            SELECT n FROM Negocio n
            JOIN FETCH n.categoria c
            LEFT JOIN FETCH c.traducciones
            JOIN FETCH n.distrito
            LEFT JOIN FETCH n.traducciones
            WHERE n.usuario.id = :usuarioId
            ORDER BY n.createdAt DESC
            """)
    List<Negocio> findMiosConDetalle(@Param("usuarioId") UUID usuarioId);
}
