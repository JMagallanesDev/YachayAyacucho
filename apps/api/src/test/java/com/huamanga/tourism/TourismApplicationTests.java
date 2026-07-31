package com.huamanga.tourism;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de humo: verifica que el contexto completo de Spring arranca.
 *
 * <p>Detecta errores de configuracion (beans mal definidos, propiedades
 * sin resolver) sin necesidad de infraestructura levantada: el perfil
 * "test" aporta valores ficticios y ni el DataSource ni Redis abren
 * conexion hasta que alguien los usa.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class TourismApplicationTests {

    @Test
    @DisplayName("el contexto de Spring arranca sin errores")
    void contextLoads() {
    }
}
