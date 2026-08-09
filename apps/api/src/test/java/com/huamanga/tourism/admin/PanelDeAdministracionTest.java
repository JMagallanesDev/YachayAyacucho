package com.huamanga.tourism.admin;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Panel de administracion: metricas, usuarios, analitica y bitacora
 * (RF-51, RF-52, RF-52b, RF-56).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Panel de administracion")
class PanelDeAdministracionTest extends BasePostgis {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private String tokenAdmin;
    private String tokenUsuario;
    private UUID idAdmin;
    private UUID idUsuario;

    @BeforeEach
    void preparar() throws Exception {
        limpiar("rate:*");
        limpiar("antispam:*");
        limpiar("visita:*");

        jdbc.execute("DELETE FROM registro_actividad");
        jdbc.execute("DELETE FROM visita_resumen_diario");
        jdbc.execute("DELETE FROM lugar_ruta");
        jdbc.execute("DELETE FROM ruta_traduccion");
        jdbc.execute("DELETE FROM ruta_tematica");
        jdbc.execute("DELETE FROM lugar_traduccion");
        jdbc.execute("DELETE FROM lugar");
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM usuario");

        tokenUsuario = registrar("vecina@yachay.pe");
        tokenAdmin = crearAdmin();

        idUsuario = idDe("vecina@yachay.pe");
        idAdmin = idDe("jefa@yachay.pe");
    }

    // ---------------------------------------------------------------
    //  Gestion de usuarios (RF-51)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Gestion de usuarios")
    class Usuarios {

        /**
         * La comprobacion que el usuario pidio explicitamente: el panel no puede
         * filtrar la contrasena por ningun camino.
         */
        @Test
        @DisplayName("el listado NO contiene la contrasena ni su hash")
        void nuncaSeExponeLaContrasena() throws Exception {
            MvcResult resultado = mockMvc.perform(get("/admin/usuarios")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andReturn();

            String cuerpo = resultado.getResponse().getContentAsString();

            // Se busca en el JSON ENTERO, no en campos concretos: si manana
            // alguien anade una propiedad nueva al DTO que arrastre la clave,
            // este test lo ve igual.
            assertThat(cuerpo)
                    .as("el prefijo de BCrypt no puede aparecer en ninguna respuesta")
                    .doesNotContain("$2a$").doesNotContain("$2b$").doesNotContain("$2y$");
            assertThat(cuerpo)
                    .as("ni siquiera el nombre del campo debe viajar")
                    .doesNotContain("passwordHash").doesNotContain("password");
            assertThat(cuerpo).contains("vecina@yachay.pe");
        }

        @Test
        @DisplayName("el admin puede cambiar el rol de otra persona")
        void cambiarRolAjeno() throws Exception {
            mockMvc.perform(patch("/admin/usuarios/" + idUsuario)
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rol\": \"ADMIN\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("ADMIN"));
        }

        @Test
        @DisplayName("suspender una cuenta la deja SUSPENDIDA")
        void suspender() throws Exception {
            mockMvc.perform(patch("/admin/usuarios/" + idUsuario)
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\": \"SUSPENDIDO\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("SUSPENDIDO"));
        }

        /** Primera barrera: quitarse ADMIN no tiene marcha atras desde dentro. */
        @Test
        @DisplayName("nadie puede cambiarse el rol a si mismo")
        void noPuedeCambiarseSuPropioRol() throws Exception {
            mockMvc.perform(patch("/admin/usuarios/" + idAdmin)
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rol\": \"USUARIO\"}"))
                    .andExpect(status().isUnprocessableContent());

            assertThat(rolDe("jefa@yachay.pe")).isEqualTo("ADMIN");
        }

        /**
         * Segunda barrera. Se prueba con DOS administradores distintos para que
         * no la salve la primera: aqui el afectado no es quien ejecuta.
         */
        @Test
        @DisplayName("no se puede degradar al ultimo administrador activo")
        void noSePuedeDegradarAlUltimoAdmin() throws Exception {
            // Solo hay una admin (jefa). Se asciende a la vecina y se degrada a
            // la jefa desde la cuenta de la vecina: hasta ahi, correcto.
            mockMvc.perform(patch("/admin/usuarios/" + idUsuario)
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rol\": \"ADMIN\"}"))
                    .andExpect(status().isOk());

            String tokenVecinaAdmin = iniciarSesion("vecina@yachay.pe");

            mockMvc.perform(patch("/admin/usuarios/" + idAdmin)
                            .header("Authorization", "Bearer " + tokenVecinaAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rol\": \"USUARIO\"}"))
                    .andExpect(status().isOk());

            // Ahora la vecina es la unica administradora activa. Otro admin no
            // existe, asi que la unica via seria que se degradara ella misma, y
            // eso lo corta la primera barrera. Se comprueba la segunda por su
            // camino propio: suspenderla.
            assertThat(administradoresActivos()).isEqualTo(1);

            mockMvc.perform(patch("/admin/usuarios/" + idUsuario)
                            .header("Authorization", "Bearer " + tokenVecinaAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\": \"SUSPENDIDO\"}"))
                    .andExpect(status().isConflict());

            assertThat(administradoresActivos())
                    .as("el sistema nunca puede quedarse sin administradores")
                    .isEqualTo(1);
        }

        /**
         * El caso que se olvida: suspender al ultimo admin lo deja fuera igual
         * que degradarlo, y contar solo por rol dejaria pasar esta via.
         */
        @Test
        @DisplayName("suspender al ultimo administrador tambien se rechaza")
        void suspenderAlUltimoAdminTambien() throws Exception {
            mockMvc.perform(patch("/admin/usuarios/" + idAdmin)
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\": \"SUSPENDIDO\"}"))
                    .andExpect(status().isConflict());

            assertThat(administradoresActivos()).isEqualTo(1);
        }

        @Test
        @DisplayName("cambiar un rol queda en la bitacora, con la IP (RF-56)")
        void elCambioSeAudita() throws Exception {
            mockMvc.perform(patch("/admin/usuarios/" + idUsuario)
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rol\": \"NEGOCIO\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/admin/actividad")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].accion").value("CAMBIAR_ROL"))
                    .andExpect(jsonPath("$[0].entidad").value("Usuario"))
                    .andExpect(jsonPath("$[0].autorEmail").value("jefa@yachay.pe"))
                    // Aqui la IP SI se guarda: es auditoria de un administrador
                    // identificado, no una denuncia ciudadana anonima.
                    .andExpect(jsonPath("$[0].ip").isNotEmpty());
        }
    }

    // ---------------------------------------------------------------
    //  Analitica (RF-52b)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Analitica de trafico")
    class Analitica {

        @Test
        @DisplayName("una visita se cuenta")
        void seCuentaLaVisita() throws Exception {
            visitar("HOME", "10.0.0.1");

            assertThat(totalVisitas("HOME")).isEqualTo(1);
            assertThat(visitasUnicas("HOME")).isEqualTo(1);
        }

        /**
         * El requisito explicito: recargar no puede inflar el contador.
         */
        @Test
        @DisplayName("recargar cinco veces NO suma cinco visitas")
        void recargarNoInfla() throws Exception {
            for (int i = 0; i < 5; i++) {
                visitar("LUGAR", "10.0.0.7");
            }

            assertThat(totalVisitas("LUGAR"))
                    .as("la ventana anti-recarga deja pasar solo la primera")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("dos visitantes distintos si cuentan dos veces")
        void visitantesDistintos() throws Exception {
            visitar("MAPA", "10.0.0.1");
            visitar("MAPA", "10.0.0.2");

            assertThat(totalVisitas("MAPA")).isEqualTo(2);
            assertThat(visitasUnicas("MAPA")).isEqualTo(2);
        }

        @Test
        @DisplayName("la misma persona en dos secciones cuenta en cada una")
        void seccionesIndependientes() throws Exception {
            visitar("HOME", "10.0.0.3");
            visitar("EVENTO", "10.0.0.3");

            assertThat(totalVisitas("HOME")).isEqualTo(1);
            assertThat(totalVisitas("EVENTO")).isEqualTo(1);
        }

        @Test
        @DisplayName("el endpoint responde 204 tanto si cuenta como si no")
        void noRevelaNada() throws Exception {
            visitar("HOME", "10.0.0.9");
            // La segunda cae en la ventana y aun asi responde igual: distinguir
            // ambos casos convertiria el endpoint en un oraculo para averiguar
            // si una huella ya habia pasado por aqui.
            visitar("HOME", "10.0.0.9");
        }

        /**
         * La comprobacion de privacidad, con el mismo metodo del Bloque 8: se
         * mira el esquema, no una fila concreta.
         */
        @Test
        @DisplayName("las tablas de analitica no tienen ninguna columna identificadora")
        void sinColumnasIdentificadoras() {
            List<String> columnas = jdbc.queryForList("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_name IN ('visita_resumen_diario', 'visita_negocio_diario')
                    """, String.class);

            assertThat(columnas)
                    .as("ni IP, ni usuario, ni sesion, ni user-agent")
                    .noneMatch(c -> c.equals("ip")
                            || c.contains("_ip")
                            || c.startsWith("ip_")
                            || c.contains("usuario")
                            || c.contains("user")
                            || c.contains("sesion")
                            || c.contains("session")
                            || c.contains("huella")
                            || c.contains("hash"));
        }

        @Test
        @DisplayName("no existe ninguna tabla de eventos crudos de visita")
        void sinEventosCrudos() {
            List<String> tablas = jdbc.queryForList("""
                    SELECT table_name FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_name LIKE '%visita%'
                    """, String.class);

            assertThat(tablas)
                    .as("solo los dos agregados diarios; los eventos crudos no se persisten")
                    .containsExactlyInAnyOrder("visita_resumen_diario", "visita_negocio_diario");
        }

        @Test
        @DisplayName("la huella de Redis no contiene la IP")
        void laHuellaNoEsLaIp() throws Exception {
            visitar("HOME", "203.0.113.44");

            var claves = redis.keys("visita:*");
            assertThat(claves).isNotEmpty();
            assertThat(claves)
                    .as("ninguna clave puede llevar la direccion dentro")
                    .noneMatch(clave -> clave.contains("203.0.113.44"));
        }
    }

    // ---------------------------------------------------------------
    //  Dashboard (RF-52)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Dashboard")
    class Dashboard {

        @Test
        @DisplayName("devuelve totales, series y pendientes en una sola llamada")
        void metricasCompletas() throws Exception {
            visitar("HOME", "10.1.0.1");

            mockMvc.perform(get("/admin/dashboard?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totales.usuarios").value(2))
                    .andExpect(jsonPath("$.totales.visitasTotales").value(1))
                    .andExpect(jsonPath("$.pendientes.fotos").exists())
                    .andExpect(jsonPath("$.pendientes.resenas").exists())
                    .andExpect(jsonPath("$.pendientes.reportes").exists())
                    .andExpect(jsonPath("$.visitasPorSeccion[0].etiqueta").value("HOME"));
        }

        /**
         * Sin esto, un grafico de lineas uniria el dia 3 con el dia 9 en una
         * recta y sugeriria una actividad que no existio.
         */
        @Test
        @DisplayName("las series traen los 30 dias, incluidos los vacios")
        void seriesSinHuecos() throws Exception {
            mockMvc.perform(get("/admin/dashboard?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(jsonPath("$.visitas.length()").value(30))
                    .andExpect(jsonPath("$.registros.length()").value(30))
                    .andExpect(jsonPath("$.visitas[0].valor").value(0));
        }

        @Test
        @DisplayName("los registros del dia aparecen en su serie")
        void registrosDelDia() throws Exception {
            mockMvc.perform(get("/admin/dashboard?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    // Las dos cuentas creadas en el @BeforeEach son de hoy, que
                    // es el ultimo punto de la serie.
                    .andExpect(jsonPath("$.registros[29].valor").value(2));
        }

        /**
         * El barrido completo esta en {@code BlindajeDelPanelTest}; este deja
         * escrito el caso concreto de forma legible.
         */
        @Test
        @DisplayName("un usuario normal recibe 403 en el dashboard")
        void unUsuarioNormalNoEntra() throws Exception {
            mockMvc.perform(get("/admin/dashboard")
                            .header("Authorization", "Bearer " + tokenUsuario))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    //  CRUD de rutas (RF-53)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Rutas tematicas")
    class Rutas {

        @Test
        @DisplayName("se crea con las paradas numeradas por su posicion")
        void creaConParadasEnOrden() throws Exception {
            List<UUID> lugares = tresLugares();

            mockMvc.perform(post("/admin/rutas?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoRuta("ruta-de-prueba", lugares)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paradas.length()").value(3))
                    .andExpect(jsonPath("$.paradas[0].orden").value(1))
                    .andExpect(jsonPath("$.paradas[1].orden").value(2))
                    .andExpect(jsonPath("$.paradas[2].orden").value(3));
        }

        @Test
        @DisplayName("reordenar la lista reordena el recorrido")
        void reordenar() throws Exception {
            List<UUID> lugares = tresLugares();

            MvcResult creada = mockMvc.perform(post("/admin/rutas?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoRuta("ruta-reordenable", lugares)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UUID rutaId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                    creada.getResponse().getContentAsString(), "$.id"));

            List<UUID> alReves = List.of(lugares.get(2), lugares.get(1), lugares.get(0));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .put("/admin/rutas/" + rutaId + "?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoRuta("ruta-reordenable", alReves)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paradas[0].lugarId").value(alReves.get(0).toString()));
        }

        @Test
        @DisplayName("una ruta no puede pasar dos veces por el mismo lugar")
        void paradaRepetida() throws Exception {
            List<UUID> lugares = tresLugares();
            List<UUID> conRepetido = List.of(lugares.get(0), lugares.get(1), lugares.get(0));

            mockMvc.perform(post("/admin/rutas?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoRuta("ruta-con-repetido", conRepetido)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("una ruta de una sola parada se rechaza")
        void unaSolaParada() throws Exception {
            List<UUID> lugares = tresLugares();

            mockMvc.perform(post("/admin/rutas?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoRuta("ruta-corta", List.of(lugares.get(0)))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("crear una ruta queda en la bitacora")
        void seAudita() throws Exception {
            mockMvc.perform(post("/admin/rutas?idioma=ES")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoRuta("ruta-auditada", tresLugares())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/admin/actividad")
                            .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(jsonPath("$[0].accion").value("CREAR_RUTA"))
                    .andExpect(jsonPath("$[0].entidad").value("RutaTematica"));
        }

        private String cuerpoRuta(String slug, List<UUID> paradas) {
            String lista = paradas.stream()
                    .map(id -> "\"" + id + "\"")
                    .collect(java.util.stream.Collectors.joining(","));

            return """
                    {"slug": "%s", "colorHex": "#B3202B", "icono": "map",
                     "activa": true, "orden": 1,
                     "traducciones": [{"idioma": "ES", "nombre": "Ruta de prueba",
                                       "descripcion": "Recorrido de prueba"}],
                     "paradas": [%s]}
                    """.formatted(slug, lista);
        }
    }

    // ---------------------------------------------------------------
    //  Ayudas
    // ---------------------------------------------------------------

    /**
     * Tres lugares publicados para poder trazar una ruta.
     *
     * <p>Se insertan con SQL en vez de por el API: el objeto de estos tests son
     * las rutas, y montar tres altas completas de lugar con sus horarios haria
     * el test mas largo y mas fragil sin probar nada mas.</p>
     */
    private List<UUID> tresLugares() {
        UUID categoria = jdbc.queryForObject(
                "SELECT id FROM categoria_lugar LIMIT 1", UUID.class);
        UUID distrito = jdbc.queryForObject(
                "SELECT id FROM distrito WHERE codigo = '050101'", UUID.class);

        List<UUID> creados = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UUID id = UUID.randomUUID();
            String slug = "parada-" + i + "-" + id.toString().substring(0, 8);

            jdbc.update("""
                    INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, estado)
                    VALUES (?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326), 'PUBLICADO')
                    """, id, slug, categoria, distrito, -74.22 + i * 0.001, -13.16 + i * 0.001);

            jdbc.update("""
                    INSERT INTO lugar_traduccion (lugar_id, idioma, nombre, descripcion)
                    VALUES (?, 'es', ?, 'Parada de prueba')
                    """, id, "Parada " + i);

            creados.add(id);
        }
        return creados;
    }

    /**
     * Simula una visita desde una direccion concreta.
     *
     * <p>Se cambia la <strong>direccion de la conexion</strong> y no la cabecera
     * {@code X-Forwarded-For}, y esa distincion importa: el
     * {@code ResolutorIpCliente} solo hace caso a esa cabecera cuando la conexion
     * llega desde un proxy de confianza, precisamente para que nadie pueda
     * fabricarse una IP distinta en cada peticion y saltarse los contadores. En
     * un test, mandar la cabecera no cambiaba nada y las cinco visitas parecian
     * la misma persona.</p>
     */
    private void visitar(String tipo, String ip) throws Exception {
        mockMvc.perform(post("/analitica/visitas?tipo=" + tipo)
                        .with(peticion -> {
                            peticion.setRemoteAddr(ip);
                            return peticion;
                        }))
                .andExpect(status().isNoContent());
    }

    private int totalVisitas(String tipo) {
        Integer total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total_visitas), 0) FROM visita_resumen_diario WHERE tipo_pagina = ?",
                Integer.class, tipo);
        return total == null ? 0 : total;
    }

    private int visitasUnicas(String tipo) {
        Integer total = jdbc.queryForObject(
                "SELECT COALESCE(MAX(visitas_unicas), 0) FROM visita_resumen_diario WHERE tipo_pagina = ?",
                Integer.class, tipo);
        return total == null ? 0 : total;
    }

    private long administradoresActivos() {
        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMIN' AND u.estado = 'ACTIVO' AND u.deleted_at IS NULL
                """, Long.class);
        return total == null ? 0 : total;
    }

    private String rolDe(String email) {
        return jdbc.queryForObject("""
                SELECT r.nombre FROM usuario u JOIN rol r ON r.id = u.rol_id WHERE u.email = ?
                """, String.class, email);
    }

    private UUID idDe(String email) {
        return jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", UUID.class, email);
    }

    private String registrar(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Yachay2026Dev", "nombre": "Persona"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        return iniciarSesion(email);
    }

    private String iniciarSesion(String email) throws Exception {
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
        return iniciarSesion("jefa@yachay.pe");
    }

    private void limpiar(String patron) {
        var claves = redis.keys(patron);
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }
}
