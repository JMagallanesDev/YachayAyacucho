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
}
