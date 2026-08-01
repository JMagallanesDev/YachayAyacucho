package com.huamanga.tourism;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de humo: verifica que el contexto completo de Spring arranca.
 *
 * <p>Desde el Bloque 1 arranca contra la base de datos real del contenedor y
 * no contra valores ficticios. No es un capricho: con JPA en el classpath,
 * Hibernate necesita una conexion viva para determinar el dialecto, asi que
 * una URL inventada ya no sirve. A cambio, esta prueba pasa a cubrir mucho
 * mas: que las 14 migraciones se aplican, que las 36 clases mapeadas encajan
 * con el esquema y que todos los beans se construyen.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Arranque de la aplicacion")
class TourismApplicationTests extends BasePostgis {

    @Test
    @DisplayName("el contexto de Spring arranca sin errores")
    void contextLoads() {
    }
}
