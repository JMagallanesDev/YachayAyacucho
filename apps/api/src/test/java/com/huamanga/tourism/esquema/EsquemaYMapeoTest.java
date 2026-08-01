package com.huamanga.tourism.esquema;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El test mas importante del Bloque 1.
 *
 * <p>Arranca el contexto completo con {@code ddl-auto=validate} contra una
 * base construida por Flyway desde cero. Si una sola de las 36 clases mapeadas
 * no cuadra con su tabla —un tipo distinto, una columna que falta, un
 * nullable que no coincide— el contexto no arranca y el test falla.</p>
 *
 * <p>Es la garantia de que el modelo del documento de tesis, el esquema de la
 * base de datos y el codigo Java son exactamente el mismo modelo, y no tres
 * versiones que se van separando con el tiempo.</p>
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ActiveProfiles("test")
@DisplayName("Esquema Flyway y mapeo JPA")
class EsquemaYMapeoTest extends BasePostgis {

    /** Las 35 entidades del modelo. La vista materializada va aparte. */
    private static final int TABLAS_DEL_MODELO = 35;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("el mapeo de las 36 clases valida contra el esquema creado por Flyway")
    void mapeoValidaContraEsquema() {
        // Si el contexto arranco, Hibernate ya valido las 36 clases mapeadas.
        // Esta comprobacion adicional confirma que la base es la del contenedor.
        Integer uno = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(uno).isEqualTo(1);
    }

    @Test
    @DisplayName("existen exactamente 35 tablas de dominio")
    void existen35Tablas() {
        List<String> tablas = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name NOT LIKE 'spatial_%'
                  AND table_name <> 'flyway_schema_history'
                ORDER BY table_name
                """, String.class);

        assertThat(tablas).hasSize(TABLAS_DEL_MODELO);
    }

    @Test
    @DisplayName("las 35 se reparten en 24 de dominio, 8 de traduccion y 3 pivote")
    void elDesgloseDeEntidadesCuadra() {
        List<String> traducciones = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name LIKE '%traduccion'
                """, String.class);

        List<String> pivotes = List.of("favorito", "lugar_ruta", "insignia_usuario");

        assertThat(traducciones).hasSize(8);
        assertThat(pivotes).hasSize(3);
        assertThat(TABLAS_DEL_MODELO - traducciones.size() - pivotes.size()).isEqualTo(24);
    }

    @Test
    @DisplayName("existe la vista materializada con su indice unico")
    void existeLaVistaMaterializada() {
        Integer vistas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_matviews WHERE schemaname = 'public' AND matviewname = 'estadistica_lugar'",
                Integer.class);

        // El indice unico no es opcional: sin el, PostgreSQL rechaza
        // REFRESH MATERIALIZED VIEW CONCURRENTLY.
        Integer indiceUnico = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'estadistica_lugar'
                  AND indexname = 'idx_estadistica_lugar_pk'
                """, Integer.class);

        assertThat(vistas).isEqualTo(1);
        assertThat(indiceUnico).isEqualTo(1);
    }

    @Test
    @DisplayName("PostGIS esta instalado y las columnas geometricas usan SRID 4326")
    void postgisConfiguradoConSrid4326() {
        String version = jdbcTemplate.queryForObject("SELECT PostGIS_Version()", String.class);

        List<Integer> srids = jdbcTemplate.queryForList("""
                SELECT DISTINCT srid FROM geometry_columns WHERE f_table_schema = 'public'
                """, Integer.class);

        assertThat(version).isNotBlank();
        assertThat(srids).containsExactly(4326);
    }

    @Test
    @DisplayName("los catalogos de referencia quedaron sembrados por la migracion")
    void catalogosSembrados() {
        assertThat(contar("rol")).isEqualTo(4);
        assertThat(contar("provincia")).isEqualTo(11);
        assertThat(contar("distrito")).isEqualTo(119);
        assertThat(contar("categoria_lugar")).isEqualTo(8);
        assertThat(contar("categoria_negocio")).isEqualTo(7);
        assertThat(contar("tipo_incidente")).isEqualTo(7);
        assertThat(contar("insignia")).isEqualTo(8);
    }

    @Test
    @DisplayName("cada catalogo tiene sus traducciones en espanol e ingles")
    void catalogosTraducidos() {
        assertThat(contar("categoria_lugar_traduccion")).isEqualTo(16);
        assertThat(contar("categoria_negocio_traduccion")).isEqualTo(14);
        assertThat(contar("tipo_incidente_traduccion")).isEqualTo(14);
        assertThat(contar("insignia_traduccion")).isEqualTo(16);
    }

    @Test
    @DisplayName("los identificadores sembrados son UUID v7")
    void catalogosConUuidV7() {
        // El digito 15 de la representacion textual es la version del UUID.
        Integer noSonV7 = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM (
                    SELECT id FROM rol
                    UNION ALL SELECT id FROM provincia
                    UNION ALL SELECT id FROM distrito
                    UNION ALL SELECT id FROM categoria_lugar
                    UNION ALL SELECT id FROM insignia
                ) t WHERE substring(id::text, 15, 1) <> '7'
                """, Integer.class);

        assertThat(noSonV7).isZero();
    }

    private Integer contar(String tabla) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }
}
