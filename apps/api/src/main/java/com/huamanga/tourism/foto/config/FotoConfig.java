package com.huamanga.tourism.foto.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra las propiedades del modulo de fotos.
 *
 * <p>Un record anotado con {@code @ConfigurationProperties} no se convierte en
 * bean por si solo: hace falta declararlo, y este es el sitio donde se ve que
 * lo hace el propio modulo y no una configuracion global lejana.</p>
 */
@Configuration
@EnableConfigurationProperties(PropiedadesCloudinary.class)
public class FotoConfig {
}
