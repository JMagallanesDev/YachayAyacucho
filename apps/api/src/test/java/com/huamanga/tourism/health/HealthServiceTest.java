package com.huamanga.tourism.health;

import com.huamanga.tourism.health.dto.ComponentStatus;
import com.huamanga.tourism.health.dto.HealthResponse;
import com.huamanga.tourism.health.dto.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthService")
class HealthServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-31T15:00:00Z");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    private HealthService healthService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        healthService = new HealthService(jdbcTemplate, redisConnectionFactory, fixedClock, "yachay-api");
    }

    @Test
    @DisplayName("reporta UP cuando PostgreSQL y Redis responden")
    void reportaUpCuandoTodoResponde() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        HealthResponse response = healthService.check();

        assertThat(response.status()).isEqualTo(HealthStatus.UP);
        assertThat(response.application()).isEqualTo("yachay-api");
        assertThat(response.timestamp()).isEqualTo(FIXED_INSTANT);
        assertThat(response.components())
                .extracting(ComponentStatus::name, ComponentStatus::status)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("postgresql", HealthStatus.UP),
                        org.assertj.core.api.Assertions.tuple("redis", HealthStatus.UP));
    }

    @Test
    @DisplayName("reporta DOWN global si PostgreSQL no responde, aunque Redis este sano")
    void reportaDownSiLaBaseDeDatosFalla() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        HealthResponse response = healthService.check();

        assertThat(response.status()).isEqualTo(HealthStatus.DOWN);
        assertThat(response.components().getFirst().status()).isEqualTo(HealthStatus.DOWN);
        assertThat(response.components().get(1).status()).isEqualTo(HealthStatus.UP);
    }

    @Test
    @DisplayName("reporta DOWN global si Redis no responde")
    void reportaDownSiRedisFalla() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(redisConnectionFactory.getConnection())
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        HealthResponse response = healthService.check();

        assertThat(response.status()).isEqualTo(HealthStatus.DOWN);
        assertThat(response.components().get(1).status()).isEqualTo(HealthStatus.DOWN);
    }

    @Test
    @DisplayName("reporta DOWN si Redis contesta algo distinto de PONG")
    void reportaDownSiRedisContestaOtraCosa() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("");

        HealthResponse response = healthService.check();

        assertThat(response.status()).isEqualTo(HealthStatus.DOWN);
    }

    @Test
    @DisplayName("cierra siempre la conexion de Redis para no filtrar el pool")
    void cierraLaConexionDeRedis() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        healthService.check();

        verify(redisConnection, times(1)).close();
    }

    @Test
    @DisplayName("no expone el mensaje real de la excepcion en la respuesta")
    void noExponeDetallesInternos() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException(
                        "FATAL: password authentication failed for user 'postgres' at 10.0.0.5:5432"));
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        HealthResponse response = healthService.check();

        assertThat(response.components().getFirst().detail())
                .isEqualTo("Sin conexion con la base de datos")
                .doesNotContain("password", "postgres", "10.0.0.5");
    }
}
