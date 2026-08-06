package com.huamanga.tourism.reporte;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Anonimato real de los reportes ciudadanos (RF-72).
 *
 * <p>Este es el test que sostiene la promesa central del modulo diferenciador.
 * No comprueba que el API <em>diga</em> que el reporte es anonimo —eso es
 * trivial— sino que <strong>mira la fila en la base de datos</strong> y verifica
 * que ninguna columna identifica a quien denuncio.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Anonimato de los reportes ciudadanos")
class AnonimatoReportesTest extends BasePostgis {

    private static final double LON = -74.2236;
    private static final double LAT = -13.1588;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private UUID tipoVandalismo;
    private String tokenAna;

    @BeforeEach
    void preparar() throws Exception {
        limpiar("rate:*");
        limpiar("antispam:*");
        limpiar("reporte:anon:*");

        jdbc.execute("DELETE FROM foto_reporte");
        jdbc.execute("DELETE FROM reporte");
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");

        tipoVandalismo = jdbc.queryForObject(
                "SELECT id FROM tipo_incidente WHERE codigo = 'VANDALISMO'", UUID.class);

        tokenAna = registrar("ana@yachay.pe");
    }

    // ---------------------------------------------------------------
    //  La prueba estrella
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Un reporte anonimo no deja rastro")
    class SinRastro {

        @Test
        @DisplayName("enviado CON SESION INICIADA, la fila no guarda ninguna identidad")
        void conSesionIniciadaSigueSiendoAnonimo() throws Exception {
            // Este es el caso que importa y el que estaba roto: alguien que usa
            // la aplicacion, tiene cuenta, y quiere denunciar algo delicado sin
            // que su nombre quede pegado. La auditoria de JPA rellenaba
            // `created_by` con su identificador aunque el reporte se marcara
            // como anonimo.
            crearReporte(tokenAna, true, "Han pintado el muro del templo");

            Map<String, Object> fila = filaUnica();

            assertThat(fila.get("usuario_id"))
                    .as("usuario_id debe ser NULL")
                    .isNull();
            assertThat(fila.get("created_by"))
                    .as("created_by delataba a quien tenia sesion iniciada")
                    .isNull();
            assertThat(fila.get("updated_by"))
                    .as("updated_by tambien lo rellena la auditoria al insertar")
                    .isNull();
            assertThat(fila.get("nombre_reportante"))
                    .as("no se guarda nombre en un reporte anonimo")
                    .isNull();
            assertThat(fila.get("es_anonimo")).isEqualTo(true);
        }

        @Test
        @DisplayName("enviado SIN cuenta, tampoco")
        void sinCuentaTampoco() throws Exception {
            crearReporte(null, true, "Basura acumulada junto al mirador");

            Map<String, Object> fila = filaUnica();
            assertThat(fila.get("usuario_id")).isNull();
            assertThat(fila.get("created_by")).isNull();
            assertThat(fila.get("nombre_reportante")).isNull();
        }

        @Test
        @DisplayName("aunque envie un nombre, se descarta")
        void ignoraElNombreEnviado() throws Exception {
            // Un cliente manipulado podria mandar el nombre igualmente. La
            // entidad lo borra en su @PrePersist, no el service.
            mockMvc.perform(multipart("/reportes")
                            .file(parteJson("""
                                    {"tipoIncidenteId": "%s", "descripcion": "Prueba",
                                     "longitud": %s, "latitud": %s,
                                     "esAnonimo": true, "nombreReportante": "Ana Perez"}
                                    """.formatted(tipoVandalismo, LON, LAT)))
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(status().isCreated());

            assertThat(filaUnica().get("nombre_reportante")).isNull();
        }

        @Test
        @DisplayName("NINGUNA columna de la tabla contiene el identificador del usuario")
        void ningunaColumnaLoContiene() throws Exception {
            UUID idAna = jdbc.queryForObject(
                    "SELECT id FROM usuario WHERE email = 'ana@yachay.pe'", UUID.class);

            crearReporte(tokenAna, true, "Construccion irregular junto a la iglesia");

            // Se vuelca la fila ENTERA a texto y se busca el identificador. Es
            // una red de seguridad frente al futuro: si alguien anade manana una
            // columna que guarde al usuario, este test lo detecta aunque nadie
            // se acuerde de actualizarlo.
            String filaCompleta = jdbc.queryForObject(
                    "SELECT reporte::text FROM reporte LIMIT 1", String.class);

            assertThat(filaCompleta)
                    .as("la fila entera no puede contener el id de quien denuncio")
                    .doesNotContain(idAna.toString());
        }

        @Test
        @DisplayName("la tabla no tiene ninguna columna de IP")
        void sinColumnasDeIp() {
            List<String> columnas = jdbc.queryForList("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_name = 'reporte'
                    """, String.class);

            // Se compara con palabras completas y no con subcadenas: "ip" vive
            // dentro de «descr-ip-cion» y de «t-ip-o_incidente_id», asi que un
            // contains() daria falsos positivos y el test no probaria nada.
            assertThat(columnas)
                    .as("ni IP ni hash de IP: una IP hasheada es reversible por fuerza bruta")
                    .noneMatch(c -> c.matches(".*(^|_)(ip|ips|hash|huella|fingerprint)(_|$).*")
                            || c.equals("ip")
                            || c.contains("ip_")
                            || c.contains("_ip")
                            || c.contains("user_agent")
                            || c.contains("remote_addr"));
        }
    }

    // ---------------------------------------------------------------
    //  El contraste: un reporte identificado SI guarda la identidad
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Un reporte identificado")
    class Identificado {

        @Test
        @DisplayName("guarda usuario y auditoria, que es lo que se espera")
        void guardaLaIdentidad() throws Exception {
            crearReporte(tokenAna, false, "Grafiti en la fachada del museo");

            Map<String, Object> fila = filaUnica();
            assertThat(fila.get("usuario_id")).isNotNull();
            assertThat(fila.get("created_by")).isNotNull();
            assertThat(fila.get("es_anonimo")).isEqualTo(false);
        }

        @Test
        @DisplayName("aparece en 'mis reportes'; el anonimo no")
        void soloLosIdentificadosSonMios() throws Exception {
            crearReporte(tokenAna, false, "Identificado");
            crearReporte(tokenAna, true, "Anonimo");

            mockMvc.perform(get("/reportes/mios").param("idioma", "ES")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].descripcion").value("Identificado"));
        }
    }

    // ---------------------------------------------------------------
    //  Validaciones y flujo
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("rechaza coordenadas fuera de Ayacucho")
        void rechazaFueraDeAyacucho() throws Exception {
            // Lima: valida como punto, invalida para este sistema.
            mockMvc.perform(multipart("/reportes")
                            .file(parteJson("""
                                    {"tipoIncidenteId": "%s", "descripcion": "Prueba",
                                     "longitud": -77.0428, "latitud": -12.0464}
                                    """.formatted(tipoVandalismo))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("fuera-de-ayacucho"));

            assertThat(contar("reporte")).isZero();
        }

        @Test
        @DisplayName("exige tipo, descripcion y ubicacion; nada mas")
        void soloTresObligatorios() throws Exception {
            // Sin direccion, sin fotos, sin nombre: debe bastar. Es lo que
            // permite completar el formulario en menos de 60 s (RF-69).
            mockMvc.perform(multipart("/reportes")
                            .file(parteJson("""
                                    {"tipoIncidenteId": "%s", "descripcion": "Lo minimo",
                                     "longitud": %s, "latitud": %s}
                                    """.formatted(tipoVandalismo, LON, LAT))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("sin descripcion no se acepta")
        void exigeDescripcion() throws Exception {
            mockMvc.perform(multipart("/reportes")
                            .file(parteJson("""
                                    {"tipoIncidenteId": "%s", "descripcion": "  ",
                                     "longitud": %s, "latitud": %s}
                                    """.formatted(tipoVandalismo, LON, LAT))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("los 7 tipos de incidente se leen sin cuenta (RF-70)")
        void tiposPublicos() throws Exception {
            mockMvc.perform(get("/reportes/tipos").param("idioma", "ES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(7))
                    .andExpect(jsonPath("$[?(@.codigo=='VANDALISMO')].nombre")
                            .value(org.hamcrest.Matchers.contains("Vandalismo")));
        }
    }

    // ---------------------------------------------------------------
    //  Mapa publico (RF-74)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Mapa publico")
    class MapaPublico {

        @Test
        @DisplayName("un reporte recien creado NO aparece: hay que aprobarlo antes")
        void soloTrasModerar() throws Exception {
            crearReporte(null, true, "Sin revisar todavia");

            mockMvc.perform(get("/reportes/mapa")
                            .param("oeste", "-75.5").param("sur", "-15.5")
                            .param("este", "-73.0").param("norte", "-12.5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("aparece cuando un administrador lo aprueba")
        void apareceTrasAprobar() throws Exception {
            String id = idDe(crearReporte(null, true, "Vandalismo confirmado"));
            String tokenAdmin = crearAdmin();

            mockMvc.perform(post("/admin/reportes/" + id + "/estado")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"estado": "APROBADO", "notas": "Verificado en campo"}
                                    """))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/reportes/mapa")
                            .param("oeste", "-75.5").param("sur", "-15.5")
                            .param("este", "-73.0").param("norte", "-12.5")
                            .param("idioma", "ES"))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].descripcion").value("Vandalismo confirmado"))
                    // Las notas internas NUNCA salen al publico.
                    .andExpect(jsonPath("$[0].notasAdmin").doesNotExist());
        }

        @Test
        @DisplayName("un reporte descartado no llega al mapa")
        void descartadoNoAparece() throws Exception {
            String id = idDe(crearReporte(null, true, "Denuncia infundada"));
            String tokenAdmin = crearAdmin();

            mockMvc.perform(post("/admin/reportes/" + id + "/estado")
                    .header("Authorization", "Bearer " + tokenAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"estado": "DESCARTADO", "notas": "No se pudo verificar"}
                            """));

            mockMvc.perform(get("/reportes/mapa")
                            .param("oeste", "-75.5").param("sur", "-15.5")
                            .param("este", "-73.0").param("norte", "-12.5"))
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("la bandeja de moderacion exige rol ADMIN")
        void bandejaSoloAdmin() throws Exception {
            mockMvc.perform(get("/admin/reportes")
                            .header("Authorization", "Bearer " + tokenAna))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/admin/reportes"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ---------------------------------------------------------------
    //  Insignia GUARDIAN
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Insignia GUARDIAN")
    class Guardian {

        @Test
        @DisplayName("se concede al aprobar un reporte identificado")
        void seConcedeConReporteIdentificado() throws Exception {
            String id = idDe(crearReporte(tokenAna, false, "Deterioro del techo"));
            String tokenAdmin = crearAdmin();

            mockMvc.perform(post("/admin/reportes/" + id + "/estado")
                    .header("Authorization", "Bearer " + tokenAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"estado": "APROBADO"}
                            """));

            assertThat(tieneGuardian("ana@yachay.pe")).isTrue();
        }

        @Test
        @DisplayName("NO se concede si el reporte era anonimo")
        void noSeConcedeConReporteAnonimo() throws Exception {
            String id = idDe(crearReporte(tokenAna, true, "Deterioro del techo"));
            String tokenAdmin = crearAdmin();

            mockMvc.perform(post("/admin/reportes/" + id + "/estado")
                    .header("Authorization", "Bearer " + tokenAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"estado": "APROBADO"}
                            """));

            // No es una limitacion tecnica: un reporte anonimo no tiene a quien
            // atribuirse. No se puede ser anonimo y recibir credito a la vez.
            assertThat(tieneGuardian("ana@yachay.pe")).isFalse();
        }
    }

    // ---------------------------------------------------------------
    //  Ayudantes
    // ---------------------------------------------------------------

    private MvcResult crearReporte(String token, boolean anonimo, String descripcion)
            throws Exception {
        var peticion = multipart("/reportes").file(parteJson("""
                {"tipoIncidenteId": "%s", "descripcion": "%s",
                 "longitud": %s, "latitud": %s, "esAnonimo": %s}
                """.formatted(tipoVandalismo, descripcion, LON, LAT, anonimo)));

        if (token != null) {
            peticion = peticion.header("Authorization", "Bearer " + token);
        }

        return mockMvc.perform(peticion).andExpect(status().isCreated()).andReturn();
    }

    /** La parte JSON del multipart; el controller la recibe como @RequestPart. */
    private MockMultipartFile parteJson(String json) {
        return new MockMultipartFile("reporte", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Map<String, Object> filaUnica() {
        return jdbc.queryForMap("SELECT * FROM reporte LIMIT 1");
    }

    private String idDe(MvcResult resultado) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                resultado.getResponse().getContentAsString(), "$.id");
    }

    private boolean tieneGuardian(String email) {
        Integer filas = jdbc.queryForObject("""
                SELECT COUNT(*) FROM insignia_usuario iu
                JOIN insignia i ON i.id = iu.insignia_id
                JOIN usuario u ON u.id = iu.usuario_id
                WHERE i.codigo = 'GUARDIAN' AND u.email = ?
                """, Integer.class, email);
        return filas != null && filas > 0;
    }

    private String registrar(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Yachay2026Dev", "nombre": "Ana"}
                                """.formatted(email)))
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

    private String crearAdmin() throws Exception {
        registrar("jefa@yachay.pe");
        jdbc.update("""
                UPDATE usuario SET rol_id = (SELECT id FROM rol WHERE nombre='ADMIN')
                WHERE email = 'jefa@yachay.pe'
                """);

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jefa@yachay.pe", "password": "Yachay2026Dev"}
                                """))
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
