package com.huamanga.tourism.health;

import com.huamanga.tourism.health.dto.ComponentStatus;
import com.huamanga.tourism.health.dto.HealthResponse;
import com.huamanga.tourism.health.dto.HealthStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@ActiveProfiles("test")
@DisplayName("GET /health")
class HealthControllerTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-07-31T15:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    @DisplayName("devuelve 200 y el detalle por componente cuando todo esta operativo")
    void devuelve200CuandoTodoEstaOperativo() throws Exception {
        when(healthService.check()).thenReturn(HealthResponse.from("yachay-api", TIMESTAMP, List.of(
                ComponentStatus.up("postgresql", 8, "Conexion establecida"),
                ComponentStatus.up("redis", 2, "PING respondido"))));

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("yachay-api"))
                .andExpect(jsonPath("$.components.length()").value(2))
                .andExpect(jsonPath("$.components[0].name").value("postgresql"))
                .andExpect(jsonPath("$.components[0].status").value("UP"))
                .andExpect(jsonPath("$.components[1].name").value("redis"));
    }

    @Test
    @DisplayName("devuelve 503 cuando algun componente esta caido")
    void devuelve503CuandoAlgoFalla() throws Exception {
        when(healthService.check()).thenReturn(HealthResponse.from("yachay-api", TIMESTAMP, List.of(
                ComponentStatus.up("postgresql", 8, "Conexion establecida"),
                ComponentStatus.down("redis", 2000, "Sin conexion con la cache"))));

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(HealthStatus.DOWN.name()))
                .andExpect(jsonPath("$.components[1].status").value("DOWN"));
    }
}
