package com.huamanga.tourism.checkin;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Favoritos, check-in, pasaporte y reportes
 * (RF-35, RF-39, RF-39b, RF-45).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Participacion del visitante")
class ParticipacionTest extends BasePostgis {

    /** Plaza Mayor de Huamanga: donde se colocan los lugares de prueba. */
    private static final double LON_PLAZA = -74.2236;
    private static final double LAT_PLAZA = -13.1588;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private String tokenAna;
    private String tokenBruno;

    @BeforeEach
    void preparar() throws Exception {
        limpiar("rate:*");
        limpiar("antispam:*");

        jdbc.execute("DELETE FROM reporte_contenido");
        jdbc.execute("DELETE FROM insignia_usuario");
        // La insignia ficticia del test de criterio desconocido: si se queda,
        // el catalogo pasa a tener 9 y rompe EsquemaYMapeoTest, que comprueba
        // que la migracion siembra exactamente 8. Los contenedores son
        // compartidos por toda la suite, asi que la basura viaja entre clases.
        jdbc.execute("DELETE FROM insignia WHERE codigo = 'INVENTADA'");
        jdbc.execute("DELETE FROM check_in");
        jdbc.execute("DELETE FROM favorito");
        jdbc.execute("DELETE FROM foto");
        jdbc.execute("DELETE FROM resena");
        jdbc.execute("DELETE FROM lugar_ruta");
        // Tambien las rutas: sin esto se acumulan entre tests y `rutas[0]` del
        // pasaporte acaba siendo una ruta sobrante de la prueba anterior.
        jdbc.execute("DELETE FROM ruta_traduccion");
        jdbc.execute("DELETE FROM ruta_tematica");
        jdbc.execute("DELETE FROM horario_lugar");
        jdbc.execute("DELETE FROM lugar_traduccion");
        jdbc.execute("DELETE FROM lugar");
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");

        UUID iglesias = categoria("IGLESIAS");
        UUID museos = categoria("MUSEOS");

        // Todos en la Plaza salvo el lejano, que esta a ~1.5 km.
        crearLugar("catedral", iglesias, "Catedral", LON_PLAZA, LAT_PLAZA);
        crearLugar("san-francisco", iglesias, "San Francisco", LON_PLAZA, LAT_PLAZA);
        crearLugar("museo", museos, "Museo", LON_PLAZA, LAT_PLAZA);
        crearLugar("lejano", iglesias, "Lugar lejano", -74.2400, -13.1700);
        // En el extremo opuesto de la region, a unos 230 km. Sirve para probar
        // el salto imposible sin salirse del CHECK de coordenadas que impone la
        // base, que no deja colocar un lugar fuera de Ayacucho.
        crearLugar("otro-extremo", iglesias, "Otro extremo", -73.2000, -15.2000);

        tokenAna = registrar("ana@yachay.pe");
        tokenBruno = registrar("bruno@yachay.pe");
    }

    // ---------------------------------------------------------------
    //  Favoritos (RF-35)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Favoritos")
    class Favoritos {

        @Test
        @DisplayName("marcar y desmarcar alterna el estado")
        void alterna() throws Exception {
            mockMvc.perform(post("/lugares/catedral/favorito")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.favorito").value(true));

            mockMvc.perform(post("/lugares/catedral/favorito")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(jsonPath("$.favorito").value(false));

            assertThat(contar("favorito")).isZero();
        }

        @Test
        @DisplayName("la lista del perfil solo trae los propios")
        void listaSoloLosPropios() throws Exception {
            marcarFavorito(tokenAna, "catedral");
            marcarFavorito(tokenAna, "museo");
            marcarFavorito(tokenBruno, "san-francisco");

            mockMvc.perform(get("/perfil/favoritos").param("idioma", "ES")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));

            mockMvc.perform(get("/perfil/favoritos").param("idioma", "ES")
                            .header("Authorization", "Bearer " + tokenBruno))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].slug").value("san-francisco"));
        }

        @Test
        @DisplayName("sin cuenta no hay favoritos")
        void exigeAutenticacion() throws Exception {
            mockMvc.perform(post("/lugares/catedral/favorito"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ---------------------------------------------------------------
    //  Check-in (RF-39)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Check-in por GPS")
    class Proximidad {

        @Test
        @DisplayName("estando encima del lugar se registra la visita")
        void aceptaCerca() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.distanciaMetros").value(0))
                    .andExpect(jsonPath("$.sellos").value(1));
        }

        @Test
        @DisplayName("a 100 m se acepta: el radio son 150")
        void aceptaDentroDelRadio() throws Exception {
            // ~0.0009 grados de latitud son unos 100 m.
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA + 0.0009, 20.0))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("a 1,5 km se rechaza con la distancia real")
        void rechazaLejos() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "lejano", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("demasiado-lejos"))
                    .andExpect(jsonPath("$.radioMetros").value(150))
                    .andExpect(jsonPath("$.distanciaMetros").isNumber());

            assertThat(contar("check_in")).isZero();
        }

        @Test
        @DisplayName("una lectura con 2 km de error se rechaza aunque la posicion cuadre")
        void rechazaPrecisionMala() throws Exception {
            // El caso del navegador que triangula por IP: las coordenadas
            // parecen correctas pero no prueban cercania a nada.
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 2000.0))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("precision-insuficiente"));
        }

        @Test
        @DisplayName("el segundo check-in en el mismo lugar el mismo dia se rechaza")
        void rechazaRepetidoElMismoDia() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isCreated());

            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("checkin-repetido"));

            assertThat(contar("check_in"))
                    .as("sin enfriamiento se podria inflar 'mas visitados' a voluntad")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("pasadas 24 h se puede volver a registrar")
        void permiteTrasElEnfriamiento() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0));
            jdbc.execute("UPDATE check_in SET created_at = created_at - INTERVAL '25 hours'");

            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isCreated());

            assertThat(contar("check_in")).isEqualTo(2);
        }

        @Test
        @DisplayName("un salto de cientos de km en un minuto se rechaza")
        void rechazaSaltoImposible() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isCreated());

            // Se envejece el anterior lo justo para que entre a juzgarse por
            // velocidad, pero no lo suficiente para que el salto sea creible.
            jdbc.execute("UPDATE check_in SET created_at = created_at - INTERVAL '2 minutes'");

            // Del centro de Huamanga al extremo sureste de la region: unos
            // 230 km en dos minutos, es decir mas de 6 000 km/h.
            mockMvc.perform(checkIn(tokenAna, "otro-extremo", -73.2000, -15.2000, 15.0))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("salto-imposible"));
        }

        @Test
        @DisplayName("se guarda el punto que envio el cliente, para poder auditarlo")
        void guardaElPuntoEnviado() throws Exception {
            double lonDesplazada = LON_PLAZA + 0.0005;
            mockMvc.perform(checkIn(tokenAna, "catedral", lonDesplazada, LAT_PLAZA, 15.0))
                    .andExpect(status().isCreated());

            Double guardada = jdbc.queryForObject(
                    "SELECT ST_X(ubicacion_gps) FROM check_in", Double.class);

            assertThat(guardada)
                    .as("guardar la posicion del lugar en vez de la enviada haria imposible auditar")
                    .isCloseTo(lonDesplazada, org.assertj.core.data.Offset.offset(0.00001));
        }
    }

    // ---------------------------------------------------------------
    //  Insignias (RF-39b)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Motor de insignias")
    class Insignias {

        @Test
        @DisplayName("el primer check-in concede PRIMER_PASO")
        void concedePrimerPaso() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.insigniasNuevas").value(
                            org.hamcrest.Matchers.hasItem("PRIMER_PASO")));
        }

        @Test
        @DisplayName("no se concede dos veces la misma insignia")
        void noDuplica() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0));

            // Un segundo check-in en otro lugar reevalua todo; PRIMER_PASO ya
            // esta concedida y no debe volver a aparecer ni duplicar la fila.
            MvcResult segundo = mockMvc.perform(
                            checkIn(tokenAna, "san-francisco", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isCreated())
                    .andReturn();

            assertThat(segundo.getResponse().getContentAsString())
                    .doesNotContain("PRIMER_PASO");

            Integer filas = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM insignia_usuario iu
                    JOIN insignia i ON i.id = iu.insignia_id
                    WHERE i.codigo = 'PRIMER_PASO'
                    """, Integer.class);
            assertThat(filas).isEqualTo(1);
        }

        @Test
        @DisplayName("HISTORIADOR exige tres museos y no se concede con menos")
        void insigniaPorCategoria() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "museo", LON_PLAZA, LAT_PLAZA, 15.0));

            assertThat(tieneInsignia("ana@yachay.pe", "HISTORIADOR"))
                    .as("un museo no bastan para HISTORIADOR, que pide tres")
                    .isFalse();
            assertThat(tieneInsignia("ana@yachay.pe", "PRIMER_PASO")).isTrue();
        }

        @Test
        @DisplayName("una insignia con un criterio desconocido no rompe el check-in")
        void criterioDesconocidoNoRompe() throws Exception {
            jdbc.update("""
                    INSERT INTO insignia (id, codigo, icono, criterio, orden)
                    VALUES (uuid_generar_v7(), 'INVENTADA', 'x',
                            '{"tipo":"ALGO_QUE_NO_EXISTE","cantidad":1}'::jsonb, 99)
                    """);

            // Lo importante: la visita se registra igual. Perder un check-in
            // real por un error de configuracion seria mucho peor que no dar
            // un premio.
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isCreated());

            assertThat(tieneInsignia("ana@yachay.pe", "INVENTADA")).isFalse();

            // Se retira para no dejar el catalogo con una insignia de mas: los
            // contenedores son compartidos por toda la suite.
            jdbc.update("DELETE FROM insignia WHERE codigo = 'INVENTADA'");
        }

        @Test
        @DisplayName("la insignia se guarda en el mismo commit que la visita")
        void mismaTransaccion() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(status().isCreated());

            // Ambas cosas estan ya en la base cuando la respuesta sale: si la
            // concesion ocurriera despues del commit, una caida en medio
            // dejaria la visita sin su insignia y sin forma de recuperarla.
            assertThat(contar("check_in")).isEqualTo(1);
            assertThat(tieneInsignia("ana@yachay.pe", "PRIMER_PASO")).isTrue();
        }
    }

    // ---------------------------------------------------------------
    //  Pasaporte (RF-39b)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Pasaporte")
    class Pasaporte {

        @Test
        @DisplayName("muestra sellos, insignias obtenidas y por obtener")
        void muestraTodo() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0));

            mockMvc.perform(get("/perfil/pasaporte").param("idioma", "ES")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sellos").value(1))
                    .andExpect(jsonPath("$.lugaresTotales").value(5))
                    .andExpect(jsonPath("$.visitas[0].slug").value("catedral"))
                    // Las ocho insignias del catalogo, obtenidas o no.
                    .andExpect(jsonPath("$.insignias.length()").value(8))
                    .andExpect(jsonPath("$.insignias[?(@.codigo=='PRIMER_PASO')].obtenida")
                            .value(org.hamcrest.Matchers.contains(true)))
                    .andExpect(jsonPath("$.insignias[?(@.codigo=='EXPLORADOR')].obtenida")
                            .value(org.hamcrest.Matchers.contains(false)));
        }

        @Test
        @DisplayName("el progreso por ruta se calcula, no se almacena")
        void progresoPorRuta() throws Exception {
            UUID ruta = crearRuta("ruta-test", "Ruta de prueba");
            anadirParada(ruta, "catedral", (short) 1);
            anadirParada(ruta, "san-francisco", (short) 2);

            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0));

            mockMvc.perform(get("/perfil/pasaporte").param("idioma", "ES")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(jsonPath("$.rutas[0].visitados").value(1))
                    .andExpect(jsonPath("$.rutas[0].total").value(2))
                    .andExpect(jsonPath("$.rutas[0].completada").value(false));

            mockMvc.perform(checkIn(tokenAna, "san-francisco", LON_PLAZA, LAT_PLAZA, 15.0));

            mockMvc.perform(get("/perfil/pasaporte").param("idioma", "ES")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(jsonPath("$.rutas[0].visitados").value(2))
                    .andExpect(jsonPath("$.rutas[0].completada").value(true));

            // Y no existe ninguna tabla ni columna de progreso: si alguien
            // anadiera un contador, este test lo delataria.
            Integer columnas = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE column_name IN ('progreso', 'lugares_visitados', 'sellos')
                    """, Integer.class);
            assertThat(columnas).isZero();
        }

        @Test
        @DisplayName("completar una ruta concede RUTA_COMPLETA")
        void completarRutaConcedeInsignia() throws Exception {
            UUID ruta = crearRuta("ruta-corta", "Ruta corta");
            anadirParada(ruta, "catedral", (short) 1);

            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0))
                    .andExpect(jsonPath("$.insigniasNuevas").value(
                            org.hamcrest.Matchers.hasItem("RUTA_COMPLETA")));
        }

        @Test
        @DisplayName("el pasaporte es siempre el del usuario del token")
        void soloElPropio() throws Exception {
            mockMvc.perform(checkIn(tokenAna, "catedral", LON_PLAZA, LAT_PLAZA, 15.0));

            mockMvc.perform(get("/perfil/pasaporte").param("idioma", "ES")
                            .header("Authorization", "Bearer " + tokenBruno))
                    .andExpect(jsonPath("$.sellos").value(0));
        }
    }

    // ---------------------------------------------------------------
    //  Reportes de contenido (RF-45)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Reportes de contenido")
    class Reportes {

        @Test
        @DisplayName("al tercer reporte distinto la resena pasa a revision y desaparece")
        void terceroActivaRevision() throws Exception {
            String resenaId = crearResena(tokenBruno, "catedral", 1, "Comentario ofensivo");

            String carlos = registrar("carlos@yachay.pe");
            String diana = registrar("diana@yachay.pe");

            mockMvc.perform(reportar(tokenAna, resenaId))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.enRevision").value(false));

            mockMvc.perform(reportar(carlos, resenaId))
                    .andExpect(jsonPath("$.enRevision").value(false));

            mockMvc.perform(reportar(diana, resenaId))
                    .andExpect(jsonPath("$.enRevision").value(true));

            String estado = jdbc.queryForObject(
                    "SELECT estado FROM resena WHERE id = ?::uuid", String.class, resenaId);
            assertThat(estado).isEqualTo("EN_REVISION");

            // Y deja de verse en la lista publica sin que nadie la borre.
            mockMvc.perform(get("/lugares/catedral/resenas"))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("la misma persona no puede reportar dos veces")
        void rechazaDuplicado() throws Exception {
            String resenaId = crearResena(tokenBruno, "catedral", 1, "Algo");

            mockMvc.perform(reportar(tokenAna, resenaId)).andExpect(status().isCreated());
            mockMvc.perform(reportar(tokenAna, resenaId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("reporte-duplicado"));

            assertThat(contar("reporte_contenido")).isEqualTo(1);
        }

        @Test
        @DisplayName("no se puede reportar la propia resena")
        void rechazaAutorreporte() throws Exception {
            String resenaId = crearResena(tokenAna, "catedral", 5, "La mia");

            mockMvc.perform(reportar(tokenAna, resenaId))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("autorreporte"));
        }

        @Test
        @DisplayName("hay que indicar exactamente una: foto o resena")
        void exigeExactamenteUna() throws Exception {
            // Ninguna de las dos.
            mockMvc.perform(post("/reportes-contenido")
                            .header("Authorization", "Bearer " + tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"motivo": "OFENSIVO"}
                                    """))
                    .andExpect(status().isBadRequest());

            // Las dos a la vez.
            String resenaId = crearResena(tokenBruno, "catedral", 1, "Algo");
            mockMvc.perform(post("/reportes-contenido")
                            .header("Authorization", "Bearer " + tokenAna)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"fotoId": "%s", "resenaId": "%s", "motivo": "OFENSIVO"}
                                    """.formatted(UUID.randomUUID(), resenaId)))
                    .andExpect(status().isBadRequest());

            assertThat(contar("reporte_contenido")).isZero();
        }

        @Test
        @DisplayName("sin cuenta no se puede reportar")
        void exigeAutenticacion() throws Exception {
            mockMvc.perform(post("/reportes-contenido")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"resenaId": "%s", "motivo": "SPAM"}
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ---------------------------------------------------------------
    //  Ayudantes
    // ---------------------------------------------------------------

    private org.springframework.test.web.servlet.RequestBuilder checkIn(
            String token, String slug, double lon, double lat, Double precision) {
        return post("/lugares/" + slug + "/check-in")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"longitud": %s, "latitud": %s, "precision": %s}
                        """.formatted(lon, lat, precision));
    }

    private org.springframework.test.web.servlet.RequestBuilder reportar(
            String token, String resenaId) {
        return post("/reportes-contenido")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resenaId": "%s", "motivo": "OFENSIVO"}
                        """.formatted(resenaId));
    }

    private void marcarFavorito(String token, String slug) throws Exception {
        mockMvc.perform(post("/lugares/" + slug + "/favorito")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    private String crearResena(String token, String slug, int nota, String comentario)
            throws Exception {
        MvcResult r = mockMvc.perform(post("/lugares/" + slug + "/resenas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"calificacion": %d, "comentario": "%s"}
                                """.formatted(nota, comentario)))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private boolean tieneInsignia(String email, String codigo) {
        Integer filas = jdbc.queryForObject("""
                SELECT COUNT(*) FROM insignia_usuario iu
                JOIN insignia i ON i.id = iu.insignia_id
                JOIN usuario u ON u.id = iu.usuario_id
                WHERE i.codigo = ? AND u.email = ?
                """, Integer.class, codigo, email);
        return filas != null && filas > 0;
    }

    private UUID categoria(String codigo) {
        return jdbc.queryForObject(
                "SELECT id FROM categoria_lugar WHERE codigo = ?", UUID.class, codigo);
    }

    private void crearLugar(String slug, UUID categoriaId, String nombre, double lon, double lat) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, estado)
                SELECT uuid_generar_v7(), ?, ?, d.id,
                       ST_SetSRID(ST_MakePoint(?, ?), 4326), 'PUBLICADO'
                FROM distrito d WHERE d.codigo='050101' RETURNING id
                """, UUID.class, slug, categoriaId, lon, lat);

        jdbc.update("INSERT INTO lugar_traduccion (lugar_id, idioma, nombre) VALUES (?, 'es', ?)",
                id, nombre);
    }

    private UUID crearRuta(String slug, String nombre) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO ruta_tematica (id, slug, color_hex, icono, activa, orden)
                VALUES (uuid_generar_v7(), ?, '#B3202B', 'flag', TRUE, 1) RETURNING id
                """, UUID.class, slug);

        jdbc.update("INSERT INTO ruta_traduccion (ruta_tematica_id, idioma, nombre) VALUES (?, 'es', ?)",
                id, nombre);
        return id;
    }

    private void anadirParada(UUID rutaId, String slugLugar, short orden) {
        jdbc.update("""
                INSERT INTO lugar_ruta (ruta_tematica_id, lugar_id, orden)
                SELECT ?, l.id, ? FROM lugar l WHERE l.slug = ?
                """, rutaId, orden, slugLugar);
    }

    private String registrar(String email) throws Exception {
        String nombre = email.substring(0, 1).toUpperCase() + email.substring(1, email.indexOf('@'));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Yachay2026Dev", "nombre": "%s"}
                                """.formatted(email, nombre)))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Yachay2026Dev"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                login.getResponse().getContentAsString(), "$.accessToken");
    }

    private void limpiar(String patron) {
        var claves = redis.keys(patron);
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }

    private Integer contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }
}
