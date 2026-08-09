package com.huamanga.tourism.lugar;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Explorador de lugares: buscar, filtrar y ordenar
 * (RF-01, RF-02, RF-04, RF-05, RF-06).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Explorar lugares")
class ExplorarLugaresTest extends BasePostgis {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID idIglesias;
    private UUID idMuseos;

    @BeforeEach
    void sembrar() {
        jdbc.execute("DELETE FROM resena");
        jdbc.execute("DELETE FROM check_in");
        jdbc.execute("DELETE FROM horario_lugar");
        jdbc.execute("DELETE FROM lugar_traduccion");
        jdbc.execute("DELETE FROM lugar");
        // La bitacora de administracion (RF-56, Bloque 10) referencia al
        // usuario, asi que hay que vaciarla antes de borrar cuentas.
        jdbc.execute("DELETE FROM registro_actividad");
        jdbc.execute("DELETE FROM usuario");

        idIglesias = jdbc.queryForObject("SELECT id FROM categoria_lugar WHERE codigo='IGLESIAS'", UUID.class);
        idMuseos = jdbc.queryForObject("SELECT id FROM categoria_lugar WHERE codigo='MUSEOS'", UUID.class);

        crear("catedral", idIglesias, "Catedral de Ayacucho",
                "Templo principal frente a la Plaza Mayor",
                "Construida en sillar por los cronistas de la colonia");
        crear("san-francisco", idIglesias, "Templo de San Francisco",
                "Iglesia franciscana con retablos dorados",
                "Alberga retablos en pan de oro del siglo XVI");
        crear("museo-memoria", idMuseos, "Museo de la Memoria",
                "Memoria del conflicto armado interno",
                "Reune testimonios de las familias afectadas");

        // Un usuario y resenas, para que los rankings tengan datos.
        UUID usuario = jdbc.queryForObject("""
                INSERT INTO usuario (id, email, password_hash, nombre, rol_id, estado)
                SELECT uuid_generar_v7(), 'critico@yachay.pe', 'hash', 'Critico', r.id, 'ACTIVO'
                FROM rol r WHERE r.nombre='USUARIO' RETURNING id
                """, UUID.class);

        calificar(usuario, "catedral", 5);
        calificar(usuario, "san-francisco", 3);
        // museo-memoria se queda sin resenas: debe salir el ultimo.

        // Check-ins, para el ranking de mas visitados.
        visitar(usuario, "museo-memoria", 4);
        visitar(usuario, "catedral", 1);

        jdbc.execute("REFRESH MATERIALIZED VIEW estadistica_lugar");
    }

    // ---------------------------------------------------------------
    //  Listado y paginacion (RF-01)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("devuelve el catalogo paginado y ordenado por nombre")
    void listaPaginadaYOrdenada() throws Exception {
        mockMvc.perform(get("/lugares").param("idioma", "ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].nombre").value("Catedral de Ayacucho"))
                .andExpect(jsonPath("$.content[1].nombre").value("Museo de la Memoria"))
                .andExpect(jsonPath("$.content[2].nombre").value("Templo de San Francisco"));
    }

    @Test
    @DisplayName("respeta el tamano de pagina")
    void respetaElTamanoDePagina() throws Exception {
        mockMvc.perform(get("/lugares").param("size", "2").param("page", "0"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/lugares").param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("cada resultado trae horarios y coordenadas para la tarjeta")
    void traeLoQueNecesitaLaTarjeta() throws Exception {
        mockMvc.perform(get("/lugares").param("idioma", "ES"))
                // Coordenadas: la distancia a pie se calcula en el navegador.
                .andExpect(jsonPath("$.content[0].longitud").exists())
                .andExpect(jsonPath("$.content[0].latitud").exists())
                // Horarios: el badge abierto/cerrado se calcula en el cliente
                // porque estas paginas se sirven cacheadas.
                .andExpect(jsonPath("$.content[0].horarios").isArray())
                .andExpect(jsonPath("$.content[0].calificacionPromedio").exists());
    }

    // ---------------------------------------------------------------
    //  Busqueda (RF-02)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("busca por texto en nombre, descripcion e historia")
    void buscaPorTexto() throws Exception {
        mockMvc.perform(get("/lugares").param("q", "franciscana").param("idioma", "ES"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("san-francisco"));

        // La palabra solo aparece en la historia, no en el nombre.
        mockMvc.perform(get("/lugares").param("q", "cronistas").param("idioma", "ES"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("catedral"));
    }

    @Test
    @DisplayName("la busqueda lematiza: 'templos' encuentra 'templo'")
    void buscaLematizando() throws Exception {
        // La configuracion 'spanish' de PostgreSQL reduce las palabras a su
        // raiz, asi que el plural encuentra el singular sin trucos.
        mockMvc.perform(get("/lugares").param("q", "templos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("un termino en blanco no filtra nada")
    void terminoEnBlancoNoFiltra() throws Exception {
        // Al borrar el buscador llega una cadena vacia; si se tratara como
        // filtro, la lista se quedaria vacia en vez de volver al catalogo.
        mockMvc.perform(get("/lugares").param("q", "   "))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("una busqueda sin resultados devuelve una pagina vacia, no un error")
    void busquedaSinResultados() throws Exception {
        mockMvc.perform(get("/lugares").param("q", "submarino"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isArray());
    }

    // ---------------------------------------------------------------
    //  Filtros (RF-04, RF-05)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("filtra por categoria")
    void filtraPorCategoria() throws Exception {
        mockMvc.perform(get("/lugares").param("categoriaId", idIglesias.toString()))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/lugares").param("categoriaId", idMuseos.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("combina busqueda y categoria (RF-05)")
    void combinaFiltros() throws Exception {
        mockMvc.perform(get("/lugares")
                        .param("q", "templo")
                        .param("categoriaId", idIglesias.toString()))
                .andExpect(jsonPath("$.totalElements").value(2));

        // El mismo texto, pero en una categoria donde no hay nada.
        mockMvc.perform(get("/lugares")
                        .param("q", "templo")
                        .param("categoriaId", idMuseos.toString()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("filtra por calificacion minima")
    void filtraPorCalificacion() throws Exception {
        mockMvc.perform(get("/lugares").param("calificacionMinima", "4"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("catedral"));
    }

    // ---------------------------------------------------------------
    //  Rankings (RF-06)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ordena por mejor valorados")
    void ordenaPorMejorValorados() throws Exception {
        mockMvc.perform(get("/lugares").param("orden", "MEJOR_VALORADOS"))
                .andExpect(jsonPath("$.content[0].slug").value("catedral"))
                .andExpect(jsonPath("$.content[0].calificacionPromedio").value(5.00))
                .andExpect(jsonPath("$.content[1].slug").value("san-francisco"));
    }

    @Test
    @DisplayName("ordena por mas visitados")
    void ordenaPorMasVisitados() throws Exception {
        mockMvc.perform(get("/lugares").param("orden", "MAS_VISITADOS"))
                .andExpect(jsonPath("$.content[0].slug").value("museo-memoria"))
                .andExpect(jsonPath("$.content[0].totalVisitas").value(4));
    }

    @Test
    @DisplayName("un lugar recien publicado aparece aunque la vista no se haya refrescado")
    void lugarNuevoApareceSinRefrescar() throws Exception {
        crear("recien-creado", idMuseos, "Recien creado", "Sin estadisticas todavia", null);
        // A proposito NO se refresca la vista materializada.

        mockMvc.perform(get("/lugares").param("q", "recien"))
                .andExpect(jsonPath("$.totalElements").value(1))
                // Sin fila en la vista, los contadores salen a cero en vez de
                // nulos, para que la tarjeta no tenga que defenderse de ellos.
                .andExpect(jsonPath("$.content[0].calificacionPromedio").value(0))
                .andExpect(jsonPath("$.content[0].totalVisitas").value(0));
    }

    // ---------------------------------------------------------------
    //  Utilidades
    // ---------------------------------------------------------------

    private void crear(String slug, UUID categoriaId, String nombre, String descripcion, String historia) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, estado)
                SELECT uuid_generar_v7(), ?, ?, d.id,
                       ST_SetSRID(ST_MakePoint(-74.2236, -13.1588), 4326), 'PUBLICADO'
                FROM distrito d WHERE d.codigo='050101' RETURNING id
                """, UUID.class, slug, categoriaId);

        jdbc.update("""
                INSERT INTO lugar_traduccion (lugar_id, idioma, nombre, descripcion, historia)
                VALUES (?, 'es', ?, ?, ?)
                """, id, nombre, descripcion, historia);
    }

    private void calificar(UUID usuarioId, String slug, int estrellas) {
        jdbc.update("""
                INSERT INTO resena (id, usuario_id, lugar_id, calificacion, estado)
                SELECT uuid_generar_v7(), ?, l.id, ?, 'PUBLICADA' FROM lugar l WHERE l.slug = ?
                """, usuarioId, estrellas, slug);
    }

    private void visitar(UUID usuarioId, String slug, int veces) {
        for (int i = 0; i < veces; i++) {
            jdbc.update("""
                    INSERT INTO check_in (id, usuario_id, lugar_id, ubicacion_gps)
                    SELECT uuid_generar_v7(), ?, l.id,
                           ST_SetSRID(ST_MakePoint(-74.2236, -13.1588), 4326)
                    FROM lugar l WHERE l.slug = ?
                    """, usuarioId, slug);
        }
    }
}
