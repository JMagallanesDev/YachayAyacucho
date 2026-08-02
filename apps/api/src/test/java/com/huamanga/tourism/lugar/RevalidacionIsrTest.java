package com.huamanga.tourism.lugar;

import com.huamanga.tourism.soporte.BasePostgis;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Webhook de revalidacion ISR (seccion 5.5).
 *
 * <p>Se levanta un servidor HTTP de juguete con la clase que ya trae la JDK,
 * para comprobar contra un destino real que el aviso sale, que lleva el
 * secreto en cabecera y —lo importante— que <strong>no sale si la operacion
 * no llego a confirmarse</strong>.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Revalidacion ISR")
class RevalidacionIsrTest extends BasePostgis {

    private static final String SECRETO = "secreto-de-prueba-para-revalidar";
    private static HttpServer servidor;

    /** Avisos recibidos: cada entrada es "secretoRecibido|cuerpo". */
    private static final List<String> avisos = new CopyOnWriteArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private UUID categoriaId;
    private UUID distritoId;
    private String tokenAdmin;

    @BeforeAll
    static void levantarServidorDePrueba() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/revalidate", intercambio -> {
            String secreto = intercambio.getRequestHeaders().getFirst("X-Revalidate-Secret");
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            avisos.add(secreto + "|" + cuerpo);
            intercambio.sendResponseHeaders(200, 0);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterAll
    static void apagarServidor() {
        servidor.stop(0);
    }

    @DynamicPropertySource
    static void configurarWebhook(DynamicPropertyRegistry registro) {
        registro.add("app.revalidacion.url",
                () -> "http://127.0.0.1:" + servidor.getAddress().getPort() + "/api/revalidate");
        registro.add("app.revalidacion.secreto", () -> SECRETO);
    }

    @BeforeEach
    void preparar() throws Exception {
        avisos.clear();
        jdbc.execute("DELETE FROM horario_lugar");
        jdbc.execute("DELETE FROM lugar_traduccion");
        jdbc.execute("DELETE FROM lugar");
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");
        var claves = redis.keys("rate:*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }

        categoriaId = jdbc.queryForObject("SELECT id FROM categoria_lugar WHERE codigo = 'IGLESIAS'", UUID.class);
        distritoId = jdbc.queryForObject("SELECT id FROM distrito WHERE codigo = '050101'", UUID.class);
        tokenAdmin = tokenAdmin();
    }

    @Test
    @DisplayName("avisa a Next.js al guardar, con el secreto en cabecera y el slug en el cuerpo")
    void avisaAlGuardar() throws Exception {
        mockMvc.perform(post("/lugares")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("templo-nuevo")))
                .andExpect(status().isCreated());

        await().atMost(java.time.Duration.ofSeconds(5)).until(() -> !avisos.isEmpty());

        assertThat(avisos).hasSize(1);
        assertThat(avisos.getFirst()).startsWith(SECRETO + "|");
        assertThat(avisos.getFirst()).contains("\"slug\":\"templo-nuevo\"");
    }

    @Test
    @DisplayName("NO avisa si la operacion fue rechazada")
    void noAvisaSiNoSeGuardo() throws Exception {
        // Coordenadas de Lima: la validacion la rechaza antes de tocar la BD.
        mockMvc.perform(post("/lugares")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("nunca-guardado").replace("-74.2236", "-77.0428")))
                .andExpect(status().isBadRequest());

        // Se espera un poco por si el aviso saliera con retraso.
        Thread.sleep(1500);

        // Si el aviso saliera dentro de la transaccion, Next regeneraria la
        // pagina de un lugar que no existe.
        assertThat(avisos).isEmpty();
    }

    @Test
    @DisplayName("tambien avisa al dar de baja un lugar")
    void avisaAlDarDeBaja() throws Exception {
        String id = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(post("/lugares")
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo("efimero")))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        await().atMost(java.time.Duration.ofSeconds(5)).until(() -> avisos.size() == 1);
        avisos.clear();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/lugares/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());

        // Sin este aviso, el CDN seguiria sirviendo una ficha ya retirada.
        await().atMost(java.time.Duration.ofSeconds(5)).until(() -> !avisos.isEmpty());
        assertThat(avisos.getFirst()).contains("\"slug\":\"efimero\"");
    }

    private String cuerpo(String slug) {
        return """
                {
                  "slug": "%s",
                  "categoriaId": "%s",
                  "distritoId": "%s",
                  "longitud": -74.2236,
                  "latitud": -13.1588,
                  "estado": "PUBLICADO",
                  "traducciones": [{"idioma": "ES", "nombre": "Templo de prueba"}],
                  "horarios": []
                }
                """.formatted(slug, categoriaId, distritoId);
    }

    private String tokenAdmin() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"revalida@yachay.pe","password":"Yachay2026","nombre":"Admin"}
                        """));
        jdbc.update("UPDATE usuario SET rol_id = (SELECT id FROM rol WHERE nombre = 'ADMIN')");

        return com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"revalida@yachay.pe","password":"Yachay2026"}
                                        """))
                        .andReturn().getResponse().getContentAsString(),
                "$.accessToken");
    }
}
