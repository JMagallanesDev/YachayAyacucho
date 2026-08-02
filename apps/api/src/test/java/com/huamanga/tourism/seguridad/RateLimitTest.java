package com.huamanga.tourism.seguridad;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Rate limiting sobre Redis (RNF-14).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Rate limiting")
class RateLimitTest extends BasePostgis {

    /** El limite en endpoints de autenticacion es de 10 por minuto. */
    private static final int LIMITE_AUTH = 10;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");
        var claves = redis.keys("rate:*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }

    @Test
    @DisplayName("corta la fuerza bruta contra el login con 429 y Retry-After")
    void cortaLaFuerzaBrutaContraElLogin() throws Exception {
        int codigoFinal = 0;
        String retryAfter = null;

        // Se simulan intentos de adivinar una contrasena.
        for (int intento = 1; intento <= LIMITE_AUTH + 3; intento++) {
            MvcResult resultado = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"victima@yachay.pe","password":"intento%d"}
                                    """.formatted(intento)))
                    .andReturn();
            codigoFinal = resultado.getResponse().getStatus();
            retryAfter = resultado.getResponse().getHeader("Retry-After");
        }

        // Sin limite, un atacante probaria miles de contrasenas por minuto.
        assertThat(codigoFinal).isEqualTo(429);
        assertThat(retryAfter).isEqualTo("60");
    }

    @Test
    @DisplayName("el contador vive en Redis, para que el limite sea comun a todas las instancias")
    void elContadorViveEnRedis() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alguien@yachay.pe","password":"Yachay2026"}
                        """));

        var claves = redis.keys("rate:auth:*");

        // Un contador en memoria se multiplicaria por el numero de replicas y
        // el limite real seria N veces mayor del declarado.
        assertThat(claves).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("el limite general es mas alto que el de autenticacion")
    void elLimiteGeneralEsMasAlto() throws Exception {
        // 15 peticiones a un endpoint normal: por encima del limite de auth
        // (10) pero muy por debajo del general (100).
        int ultimoCodigo = 0;
        for (int i = 0; i < 15; i++) {
            ultimoCodigo = mockMvc.perform(post("/auth/refresh"))
                    .andReturn().getResponse().getStatus();
        }

        // 401 por no traer cookie, no 429: el limite general no se alcanzo.
        assertThat(ultimoCodigo).isEqualTo(401);
    }
}
