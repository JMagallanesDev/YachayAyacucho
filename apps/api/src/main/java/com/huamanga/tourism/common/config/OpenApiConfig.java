package com.huamanga.tourism.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentacion OpenAPI (Swagger UI).
 *
 * <p>Disponible en {@code http://localhost:8080/api/v1/swagger-ui}.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI yachayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yachay Ayacucho API")
                        .version("v1")
                        .description("""
                                API del sistema web de turismo y patrimonio cultural de Huamanga, Ayacucho.
                                Tesis: "Aplicacion web para la publicacion de informacion del patrimonio
                                cultural de Ayacucho, 2026".
                                """)
                        .contact(new Contact().name("Yachay Ayacucho"))
                        .license(new License().name("Uso academico")));
    }
}
