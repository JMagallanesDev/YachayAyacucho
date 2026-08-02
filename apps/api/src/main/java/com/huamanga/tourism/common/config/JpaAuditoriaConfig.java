package com.huamanga.tourism.common.config;

import com.huamanga.tourism.common.seguridad.UsuarioActual;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.UUID;

/**
 * Rellena automaticamente las columnas de auditoria.
 *
 * <p>Desde el Bloque 2 este {@code AuditorAware} lee el usuario del
 * SecurityContext, de modo que {@code created_by} y {@code updated_by} se
 * completan solos en las 6 entidades auditables —Lugar, Evento, RutaTematica,
 * Negocio, Reporte y las que hereden de {@code EntidadAuditable}— sin que
 * ningun service tenga que acordarse de asignarlos.</p>
 *
 * <p>Devuelve vacio en las operaciones anonimas (un reporte ciudadano sin
 * cuenta, un job programado), y entonces las columnas quedan a null, que es
 * exactamente lo que deben reflejar: nadie identificable hizo el cambio.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorActual")
public class JpaAuditoriaConfig {

    @Bean
    public AuditorAware<UUID> auditorActual() {
        return UsuarioActual::id;
    }
}
