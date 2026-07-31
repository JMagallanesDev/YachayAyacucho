package com.huamanga.tourism.health;

import com.huamanga.tourism.health.dto.ComponentStatus;
import com.huamanga.tourism.health.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

/**
 * Comprueba que el backend puede realmente hablar con su infraestructura.
 *
 * <p>No basta con que el proceso Java este vivo: si PostgreSQL o Redis no
 * responden, el sistema no puede servir contenido patrimonial. Por eso se
 * ejecuta una consulta real contra cada uno en lugar de mirar solo si el
 * bean existe.</p>
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private static final String DATABASE_COMPONENT = "postgresql";
    private static final String CACHE_COMPONENT = "redis";
    private static final String EXPECTED_PONG = "PONG";

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final Clock clock;
    private final String applicationName;

    public HealthService(JdbcTemplate jdbcTemplate,
                         RedisConnectionFactory redisConnectionFactory,
                         Clock clock,
                         @Value("${spring.application.name}") String applicationName) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.clock = clock;
        this.applicationName = applicationName;
    }

    /**
     * Ejecuta todas las comprobaciones y arma la respuesta.
     */
    public HealthResponse check() {
        List<ComponentStatus> components = List.of(checkDatabase(), checkCache());
        return HealthResponse.from(applicationName, clock.instant(), components);
    }

    /**
     * Consulta trivial contra PostgreSQL. Verifica el trayecto completo:
     * pool de HikariCP, red, autenticacion y motor.
     */
    private ComponentStatus checkDatabase() {
        long startNanos = System.nanoTime();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ComponentStatus.up(DATABASE_COMPONENT, elapsedMillis(startNanos), "Conexion establecida");
        } catch (Exception ex) {
            // El mensaje real puede incluir host, puerto y usuario: se queda en el log.
            log.warn("Comprobacion de salud de PostgreSQL fallida", ex);
            return ComponentStatus.down(DATABASE_COMPONENT, elapsedMillis(startNanos), "Sin conexion con la base de datos");
        }
    }

    /**
     * PING contra Redis. Se cierra la conexion siempre (try-with-resources)
     * para no filtrar conexiones del pool de Lettuce.
     */
    private ComponentStatus checkCache() {
        long startNanos = System.nanoTime();
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String reply = connection.ping();
            if (!EXPECTED_PONG.equalsIgnoreCase(reply)) {
                log.warn("Redis respondio al PING con un valor inesperado: {}", reply);
                return ComponentStatus.down(CACHE_COMPONENT, elapsedMillis(startNanos), "Respuesta inesperada al PING");
            }
            return ComponentStatus.up(CACHE_COMPONENT, elapsedMillis(startNanos), "PING respondido");
        } catch (Exception ex) {
            log.warn("Comprobacion de salud de Redis fallida", ex);
            return ComponentStatus.down(CACHE_COMPONENT, elapsedMillis(startNanos), "Sin conexion con la cache");
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
