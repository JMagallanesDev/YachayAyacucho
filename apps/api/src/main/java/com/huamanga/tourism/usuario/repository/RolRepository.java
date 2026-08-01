package com.huamanga.tourism.usuario.repository;

import com.huamanga.tourism.usuario.domain.NombreRol;
import com.huamanga.tourism.usuario.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {

    Optional<Rol> findByNombre(NombreRol nombre);
}
