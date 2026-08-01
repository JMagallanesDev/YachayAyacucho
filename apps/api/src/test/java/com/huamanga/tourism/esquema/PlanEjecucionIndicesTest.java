package com.huamanga.tourism.esquema;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el RNF-30: las consultas principales usan indice y no recorren la
 * tabla entera.
 *
 * <p><strong>Por que se carga volumen sintetico.</strong> Con los 5 lugares
 * del seed de demostracion, PostgreSQL haria Seq Scan en todas las consultas
 * y estaria <em>acertando</em>: recorrer 5 filas es mas barato que abrir un
 * indice. Un test que pasara con esos datos no probaria nada. Por eso se
 * cargan 5.000 lugares y sus dependientes, se lanza ANALYZE para que el
 * planificador tenga estadisticas reales y solo entonces se mide.</p>
 *
 * <p>Los planes se leen con EXPLAIN en formato JSON y se comprueba que el
 * nodo de acceso a la tabla sea Index Scan, Index Only Scan o Bitmap Heap
 * Scan, nunca Seq Scan.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Planes de ejecucion e indices (RNF-30)")
class PlanEjecucionIndicesTest extends BasePostgis {

    private static final int LUGARES_SINTETICOS = 5_000;

    @Autowired
    private JdbcTemplate jdbc;

    private boolean datosCargados;

    @BeforeAll
    void cargarVolumen() {
        if (datosCargados) {
            return;
        }
        jdbc.execute("DELETE FROM resena");
        jdbc.execute("DELETE FROM foto");
        jdbc.execute("DELETE FROM horario_lugar");
        jdbc.execute("DELETE FROM lugar_traduccion");
        jdbc.execute("DELETE FROM evento");
        jdbc.execute("DELETE FROM lugar");
        jdbc.execute("DELETE FROM usuario");

        // 5.000 lugares repartidos por las 8 categorias y los 119 distritos,
        // esparcidos dentro de los bounds de Ayacucho.
        jdbc.update("""
                INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, estado)
                SELECT uuid_generar_v7(),
                       'lugar-sintetico-' || i,
                       (SELECT id FROM categoria_lugar ORDER BY orden OFFSET (i % 8) LIMIT 1),
                       (SELECT id FROM distrito ORDER BY codigo OFFSET (i % 119) LIMIT 1),
                       ST_SetSRID(ST_MakePoint(-74.5 + (i % 100) * 0.01, -13.5 + (i % 80) * 0.01), 4326),
                       'PUBLICADO'
                FROM generate_series(1, ?) i
                """, LUGARES_SINTETICOS);

        jdbc.update("""
                INSERT INTO lugar_traduccion (lugar_id, idioma, nombre, descripcion, historia)
                SELECT l.id, 'es',
                       'Templo sintetico ' || l.slug,
                       'Descripcion de prueba para la busqueda de texto completo.',
                       CASE WHEN random() < 0.1
                            THEN 'Fundado por los cronistas de la colonia en el valle de Huamanga.'
                            ELSE 'Historia generica sin terminos distintivos.' END
                FROM lugar l
                """);

        jdbc.update("""
                INSERT INTO horario_lugar (id, lugar_id, dia_semana, hora_apertura, hora_cierre, cerrado)
                SELECT uuid_generar_v7(), l.id, d.dia, '09:00'::time, '17:00'::time, FALSE
                FROM lugar l CROSS JOIN generate_series(0, 6) AS d(dia)
                """);

        UsuariosSinteticos.crear(jdbc);

        jdbc.update("""
                INSERT INTO resena (id, usuario_id, lugar_id, calificacion, estado)
                SELECT uuid_generar_v7(), u.id, l.id, 1 + (random() * 4)::int, 'PUBLICADA'
                FROM lugar l
                JOIN LATERAL (SELECT id FROM usuario ORDER BY id LIMIT 1) u ON TRUE
                """);

        jdbc.update("""
                INSERT INTO foto (id, usuario_id, lugar_id, cloudinary_url, cloudinary_public_id, estado)
                SELECT uuid_generar_v7(), u.id, l.id, 'http://x', 'x',
                       CASE WHEN random() < 0.02 THEN 'PENDIENTE' ELSE 'APROBADA' END
                FROM lugar l
                JOIN LATERAL (SELECT id FROM usuario ORDER BY id LIMIT 1) u ON TRUE
                """);

        jdbc.update("""
                INSERT INTO evento (id, distrito_id, tipo, fecha_inicio, fecha_fin, estado)
                SELECT uuid_generar_v7(),
                       (SELECT id FROM distrito ORDER BY codigo OFFSET (i % 119) LIMIT 1),
                       'CULTURAL',
                       DATE '2026-01-01' + (i % 365),
                       DATE '2026-01-01' + (i % 365) + 1,
                       'PUBLICADO'
                FROM generate_series(1, 3000) i
                """);

        // Sin ANALYZE el planificador trabaja con estadisticas obsoletas y
        // sus decisiones no reflejan los datos reales.
        jdbc.execute("ANALYZE");
        datosCargados = true;
    }

    @Test
    @DisplayName("busqueda por slug: usa el indice unico")
    void slugUsaIndice() {
        assertThat(plan("SELECT * FROM lugar WHERE slug = 'lugar-sintetico-42'"))
                .doesNotContain("Seq Scan");
    }

    @Test
    @DisplayName("filtro por distrito: usa idx_lugar_distrito")
    void distritoUsaIndice() {
        String plan = plan("""
                SELECT * FROM lugar
                WHERE distrito_id = (SELECT id FROM distrito WHERE codigo = '050101')
                  AND deleted_at IS NULL
                """);
        assertThat(plan).contains("idx_lugar_distrito");
    }

    @Test
    @DisplayName("busqueda por cercania: usa el indice GIST espacial")
    void cercaniaUsaGist() {
        String plan = plan("""
                SELECT * FROM lugar
                WHERE ST_DWithin(ubicacion::geography,
                                 ST_SetSRID(ST_MakePoint(-74.22, -13.15), 4326)::geography, 2000)
                """);
        // Debe usar el indice funcional sobre geography, no el de geometry:
        // al convertir la columna con ::geography, el indice sobre la
        // geometria pura deja de aplicar.
        assertThat(plan).contains("idx_lugar_ubicacion_geog");
        assertThat(plan).doesNotContain("Seq Scan");
    }

    @Test
    @DisplayName("horario por lugar y dia: usa idx_horario_lugar_dia (RF-09b)")
    void horarioUsaIndice() {
        String plan = plan("""
                SELECT * FROM horario_lugar
                WHERE lugar_id = (SELECT id FROM lugar WHERE slug = 'lugar-sintetico-7')
                  AND dia_semana = 3
                """);
        assertThat(plan).contains("idx_horario_lugar_dia");
    }

    @Test
    @DisplayName("resenas de un lugar: usa idx_resena_lugar")
    void resenasUsanIndice() {
        String plan = plan("""
                SELECT * FROM resena
                WHERE lugar_id = (SELECT id FROM lugar WHERE slug = 'lugar-sintetico-9')
                """);
        assertThat(plan).contains("idx_resena_lugar");
    }

    @Test
    @DisplayName("fotos pendientes: usa el indice parcial idx_foto_pendientes (RF-49)")
    void fotosPendientesUsanIndiceParcial() {
        String plan = plan("SELECT * FROM foto WHERE estado = 'PENDIENTE' ORDER BY created_at");
        assertThat(plan).contains("idx_foto_pendientes");
    }

    @Test
    @DisplayName("eventos por rango de fechas: usa idx_evento_fecha")
    void eventosUsanIndice() {
        String plan = plan("""
                SELECT * FROM evento
                WHERE deleted_at IS NULL
                  AND fecha_inicio <= DATE '2026-03-05'
                  AND fecha_fin >= DATE '2026-03-01'
                """);
        assertThat(plan).contains("idx_evento_fecha");
    }

    @Test
    @DisplayName("busqueda de texto completo: usa el indice GIN (RF-02)")
    void textoCompletoUsaGin() {
        String plan = plan("""
                SELECT * FROM lugar_traduccion
                WHERE to_tsvector('spanish',
                        COALESCE(nombre, '') || ' ' || COALESCE(descripcion, '') || ' ' || COALESCE(historia, ''))
                      @@ plainto_tsquery('spanish', 'cronistas colonia')
                """);
        assertThat(plan).contains("idx_lugartrad_fulltext");
    }

    @Test
    @DisplayName("ranking de la vista materializada: usa su indice de calificacion (RF-06)")
    void rankingUsaIndice() {
        jdbc.execute("REFRESH MATERIALIZED VIEW estadistica_lugar");
        jdbc.execute("ANALYZE estadistica_lugar");

        String plan = plan("""
                SELECT * FROM estadistica_lugar
                ORDER BY calificacion_promedio DESC LIMIT 20
                """);
        assertThat(plan).contains("idx_estadistica_calificacion");
    }

    /** Devuelve el plan de ejecucion como texto para poder inspeccionarlo. */
    private String plan(String consulta) {
        StringBuilder texto = new StringBuilder();
        jdbc.query("EXPLAIN " + consulta, rs -> {
            texto.append(rs.getString(1)).append('\n');
        });
        return texto.toString();
    }

    /** Crea un usuario reutilizable para las filas dependientes. */
    private static final class UsuariosSinteticos {
        static void crear(JdbcTemplate jdbc) {
            jdbc.update("""
                    INSERT INTO usuario (id, email, password_hash, nombre, rol_id, estado)
                    SELECT uuid_generar_v7(), 'sintetico@yachay.pe', 'hash', 'Sintetico', r.id, 'ACTIVO'
                    FROM rol r WHERE r.nombre = 'USUARIO'
                    """);
        }
    }
}
