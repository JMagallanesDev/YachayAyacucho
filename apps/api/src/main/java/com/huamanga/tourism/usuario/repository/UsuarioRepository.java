package com.huamanga.tourism.usuario.repository;

import com.huamanga.tourism.usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Trae el rol en la misma consulta: se usa en cada autenticacion, donde
     * necesitar el rol es la norma y no la excepcion.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.email = :email")
    Optional<Usuario> findByEmailConRol(String email);

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Administradores que hoy pueden entrar al panel (RF-51, Bloque 10).
     *
     * <p>Exige rol ADMIN <strong>y</strong> estado ACTIVO: un administrador
     * suspendido deja el sistema tan bloqueado como uno degradado, y contar solo
     * por rol dejaria pasar justo esa via. Sostiene la barrera que impide apagar
     * al ultimo administrador.</p>
     */
    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE u.rol.nombre = com.huamanga.tourism.usuario.domain.NombreRol.ADMIN
              AND u.estado = com.huamanga.tourism.usuario.domain.EstadoUsuario.ACTIVO
              AND u.deletedAt IS NULL
            """)
    long contarAdministradoresActivos();
}
