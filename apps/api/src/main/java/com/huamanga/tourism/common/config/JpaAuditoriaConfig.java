package com.huamanga.tourism.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

/**
 * Rellena automaticamente las columnas de auditoria.
 *
 * <p>{@code created_at} y {@code updated_at} funcionan desde ya.
 * {@code created_by} y {@code updated_by} quedan a null hasta el Bloque 2:
 * sin autenticacion todavia no hay a quien atribuir el cambio. Cuando exista
 * Spring Security, este {@code AuditorAware} leera el usuario del
 * SecurityContext y las columnas se llenaran solas en todas las entidades a
 * la vez, sin tocar ni un service.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorActual")
public class JpaAuditoriaConfig {

    @Bean
    public AuditorAware<UUID> auditorActual() {
        return Optional::empty;
    }
}
