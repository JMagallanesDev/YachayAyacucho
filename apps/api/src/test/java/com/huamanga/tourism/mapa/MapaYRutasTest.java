package com.huamanga.tourism.mapa;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Datos del mapa: GeoJSON de lugares y rutas tematicas
 * (RF-17, RF-18, RF-20).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Mapa y rutas")
class MapaYRutasTest extends BasePostgis {

    /** Plaza Mayor de Huamanga. */
    private static final double LON_PLAZA = -74.2236;
    private static final double LAT_PLAZA = -13.1588;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void sembrar() {
        jdbc.execute("DELETE FROM lugar_ruta");
        jdbc.execute("DELETE FROM ruta_traduccion");
        jdbc.execute("DELETE FROM ruta_tematica");
        jdbc.execute("DELETE FROM horario_lugar");
        jdbc.execute("DELETE FROM lugar_traduccion");
        jdbc.execute("DELETE FROM lugar");

        UUID iglesias = jdbc.queryForObject(
                "SELECT id FROM categoria_lugar WHERE codigo='IGLESIAS'", UUID.class);
        UUID miradores = jdbc.queryForObject(
                "SELECT id FROM categoria_lugar WHERE codigo='MIRADORES'", UUID.class);

        crear("catedral", iglesias, "Catedral de Ayacucho", "Ayacucho Cathedral",
                LON_PLAZA, LAT_PLAZA, "PUBLICADO");
        crear("acuchimay", miradores, "Mirador de Acuchimay", "Acuchimay Viewpoint",
                -74.2200, -13.1700, "PUBLICADO");
        // Un borrador: no debe salir ni en el mapa ni en el recorrido.
        crear("en-obras", iglesias, "Templo en restauracion", "Temple under restoration",
                -74.2300, -13.1500, "BORRADOR");

        crearRuta("ruta-prueba", "Ruta de prueba", "Test route");
        anadirParada("ruta-prueba", "acuchimay", (short) 2);
        anadirParada("ruta-prueba", "catedral", (short) 1);
        anadirParada("ruta-prueba", "en-obras", (short) 3);
    }

    // ---------------------------------------------------------------
    //  GeoJSON para MapLibre (RF-17, RF-18)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GeoJSON del mapa")
    class Geo {

        @Test
        @DisplayName("devuelve un FeatureCollection valido")
        void formatoGeoJson() throws Exception {
            mockMvc.perform(get("/lugares/mapa").param("idioma", "ES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("FeatureCollection"))
                    .andExpect(jsonPath("$.features").isArray())
                    .andExpect(jsonPath("$.features[0].type").value("Feature"))
                    .andExpect(jsonPath("$.features[0].geometry.type").value("Point"));
        }

        @Test
        @DisplayName("las coordenadas van en orden [longitud, latitud]")
        void ordenDeCoordenadas() throws Exception {
            // El error clasico de GeoJSON es invertirlas. Con Huamanga en
            // (-74.22, -13.15), intercambiarlas la mandaria al oceano Indico,
            // asi que se comprueba el signo y la magnitud de cada una.
            mockMvc.perform(get("/lugares/mapa").param("idioma", "ES"))
                    .andExpect(jsonPath("$.features[0].geometry.coordinates[0]").value(LON_PLAZA))
                    .andExpect(jsonPath("$.features[0].geometry.coordinates[1]").value(LAT_PLAZA));
        }

        @Test
        @DisplayName("solo incluye lugares publicados")
        void ocultaBorradores() throws Exception {
            mockMvc.perform(get("/lugares/mapa"))
                    .andExpect(jsonPath("$.features.length()").value(2))
                    .andExpect(jsonPath("$.features[*].properties.slug",
                            org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("en-obras"))));
        }

        @Test
        @DisplayName("cada chincheta trae color y categoria para pintarse")
        void propiedadesDeLaChincheta() throws Exception {
            mockMvc.perform(get("/lugares/mapa").param("idioma", "ES"))
                    .andExpect(jsonPath("$.features[0].properties.slug").value("catedral"))
                    .andExpect(jsonPath("$.features[0].properties.nombre").value("Catedral de Ayacucho"))
                    .andExpect(jsonPath("$.features[0].properties.categoriaCodigo").value("IGLESIAS"))
                    .andExpect(jsonPath("$.features[0].properties.color").exists());
        }

        @Test
        @DisplayName("traduce el nombre al idioma pedido")
        void traduce() throws Exception {
            mockMvc.perform(get("/lugares/mapa").param("idioma", "EN"))
                    .andExpect(jsonPath("$.features[0].properties.nombre").value("Ayacucho Cathedral"));
        }

        @Test
        @DisplayName("es publico: el mapa se ve sin cuenta")
        void esPublico() throws Exception {
            mockMvc.perform(get("/lugares/mapa")).andExpect(status().isOk());
        }
    }

    // ---------------------------------------------------------------
    //  Rutas tematicas (RF-20)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Rutas tematicas")
    class Rutas {

        @Test
        @DisplayName("las paradas vienen en orden de recorrido")
        void paradasOrdenadas() throws Exception {
            // Se insertaron desordenadas a proposito (acuchimay antes que la
            // catedral). Unir los puntos en el orden de insercion dibujaria un
            // garabato sobre la ciudad.
            mockMvc.perform(get("/rutas").param("idioma", "ES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].paradas[0].slug").value("catedral"))
                    .andExpect(jsonPath("$[0].paradas[1].slug").value("acuchimay"));
        }

        @Test
        @DisplayName("cada parada trae sus coordenadas para la polilinea")
        void paradasConCoordenadas() throws Exception {
            mockMvc.perform(get("/rutas").param("idioma", "ES"))
                    .andExpect(jsonPath("$[0].paradas[0].longitud").value(LON_PLAZA))
                    .andExpect(jsonPath("$[0].paradas[0].latitud").value(LAT_PLAZA));
        }

        @Test
        @DisplayName("un lugar sin publicar no aparece en el recorrido")
        void excluyeNoPublicados() throws Exception {
            // Si apareciera, la polilinea llevaria a una ficha que devuelve 404.
            mockMvc.perform(get("/rutas").param("idioma", "ES"))
                    .andExpect(jsonPath("$[0].paradas.length()").value(2));
        }

        @Test
        @DisplayName("devuelve nombre, color e icono para dibujarla")
        void datosDeLaRuta() throws Exception {
            mockMvc.perform(get("/rutas").param("idioma", "ES"))
                    .andExpect(jsonPath("$[0].nombre").value("Ruta de prueba"))
                    .andExpect(jsonPath("$[0].colorHex").exists())
                    .andExpect(jsonPath("$[0].icono").exists());
        }

        @Test
        @DisplayName("traduce al idioma pedido")
        void traduce() throws Exception {
            mockMvc.perform(get("/rutas").param("idioma", "EN"))
                    .andExpect(jsonPath("$[0].nombre").value("Test route"));
        }

        @Test
        @DisplayName("una ruta inexistente devuelve 404")
        void rutaInexistente() throws Exception {
            mockMvc.perform(get("/rutas/no-existe")).andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------
    //  Ayudantes
    // ---------------------------------------------------------------

    private void crear(String slug, UUID categoriaId, String nombreEs, String nombreEn,
                       double longitud, double latitud, String estado) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, estado)
                SELECT uuid_generar_v7(), ?, ?, d.id,
                       ST_SetSRID(ST_MakePoint(?, ?), 4326), ?
                FROM distrito d WHERE d.codigo='050101' RETURNING id
                """, UUID.class, slug, categoriaId, longitud, latitud, estado);

        jdbc.update("INSERT INTO lugar_traduccion (lugar_id, idioma, nombre) VALUES (?, 'es', ?)",
                id, nombreEs);
        jdbc.update("INSERT INTO lugar_traduccion (lugar_id, idioma, nombre) VALUES (?, 'en', ?)",
                id, nombreEn);
    }

    private void crearRuta(String slug, String nombreEs, String nombreEn) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO ruta_tematica (id, slug, color_hex, icono, activa, orden)
                VALUES (uuid_generar_v7(), ?, '#B3202B', 'church', TRUE, 1) RETURNING id
                """, UUID.class, slug);

        jdbc.update("INSERT INTO ruta_traduccion (ruta_tematica_id, idioma, nombre) VALUES (?, 'es', ?)",
                id, nombreEs);
        jdbc.update("INSERT INTO ruta_traduccion (ruta_tematica_id, idioma, nombre) VALUES (?, 'en', ?)",
                id, nombreEn);
    }

    private void anadirParada(String rutaSlug, String lugarSlug, short orden) {
        jdbc.update("""
                INSERT INTO lugar_ruta (ruta_tematica_id, lugar_id, orden)
                SELECT r.id, l.id, ? FROM ruta_tematica r, lugar l
                WHERE r.slug = ? AND l.slug = ?
                """, orden, rutaSlug, lugarSlug);
    }
}
