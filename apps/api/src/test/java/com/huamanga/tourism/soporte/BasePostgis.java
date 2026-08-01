package com.huamanga.tourism.soporte;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
// Testcontainers 2.x movio los modulos a su propio paquete; la clase antigua
// org.testcontainers.containers.PostgreSQLContainer quedo deprecada.
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de los tests que necesitan una base de datos real.
 *
 * <p>Se usa PostgreSQL con PostGIS y no una base en memoria como H2 porque el
 * modelo depende de cosas que H2 no tiene: tipos {@code geometry}, indices
 * GIST, indices parciales, vistas materializadas con REFRESH CONCURRENTLY y
 * busqueda de texto completo en espanol. Probar contra H2 daria una falsa
 * sensacion de seguridad.</p>
 *
 * <p>El contenedor es un singleton estatico y <strong>no</strong> se declara
 * con {@code @Container}: asi arranca una sola vez para toda la suite en lugar
 * de una vez por clase. Docker lo retira al terminar la JVM.</p>
 */
public abstract class BasePostgis {

    protected static final PostgreSQLContainer POSTGIS;

    static {
        POSTGIS = new PostgreSQLContainer(
                DockerImageName.parse("postgis/postgis:16-3.5")
                        .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("yachay_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(false);
        POSTGIS.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGIS::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGIS::getUsername);
        registro.add("spring.datasource.password", POSTGIS::getPassword);
        // Flyway construye el esquema completo desde cero en cada arranque de
        // la suite: es tambien la prueba de que las 14 migraciones corren
        // limpias sobre una base vacia.
        registro.add("spring.flyway.enabled", () -> "true");
    }
}
