package com.huamanga.tourism.esquema;

import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba que la base de datos rechaza por si sola los datos invalidos.
 *
 * <p>Estas reglas tambien se validaran en la capa de aplicacion, pero se
 * prueban aqui contra PostgreSQL porque la aplicacion puede reescribirse y la
 * base sigue siendo la ultima linea de defensa: un INSERT por SQL directo, una
 * migracion futura mal hecha o un bug en un service tienen que chocar contra
 * estos CHECK.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Constraints de integridad en PostgreSQL")
class ConstraintsIntegridadTest extends BasePostgis {

    @Autowired
    private JdbcTemplate jdbc;

    private UUID usuarioId;
    private UUID lugarId;

    @BeforeEach
    void prepararDatos() {
        jdbc.execute("DELETE FROM resena");
        jdbc.execute("DELETE FROM reporte_contenido");
        jdbc.execute("DELETE FROM horario_lugar");
        jdbc.execute("DELETE FROM lugar_traduccion");
        jdbc.execute("DELETE FROM lugar_imagen_historica");
        jdbc.execute("DELETE FROM lugar");
        jdbc.execute("DELETE FROM usuario");

        usuarioId = jdbc.queryForObject("""
                INSERT INTO usuario (id, email, password_hash, nombre, rol_id, estado)
                SELECT uuid_generar_v7(), 'prueba@yachay.pe', 'hash', 'Prueba', r.id, 'ACTIVO'
                FROM rol r WHERE r.nombre = 'USUARIO' RETURNING id
                """, UUID.class);

        lugarId = jdbc.queryForObject("""
                INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, estado)
                SELECT uuid_generar_v7(), 'lugar-prueba', c.id, d.id,
                       ST_SetSRID(ST_MakePoint(-74.22, -13.16), 4326), 'PUBLICADO'
                FROM categoria_lugar c, distrito d
                WHERE c.codigo = 'IGLESIAS' AND d.codigo = '050101' RETURNING id
                """, UUID.class);
    }

    @Nested
    @DisplayName("Resena")
    class ResenaTest {

        @Test
        @DisplayName("rechaza una calificacion fuera del rango 1-5")
        void rechazaCalificacionFueraDeRango() {
            assertThatThrownBy(() -> insertarResena(6, "Excelente"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> insertarResena(0, "Pesimo"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("acepta las calificaciones validas")
        void aceptaCalificacionesValidas() {
            assertThatCode(() -> insertarResena(1, "Regular")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rechaza un comentario de mas de 500 caracteres")
        void rechazaComentarioLargo() {
            assertThatThrownBy(() -> insertarResena(5, "x".repeat(501)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("impide dos resenas del mismo usuario sobre el mismo lugar")
        void impideResenaDuplicada() {
            insertarResena(5, "Primera");
            // Sin este UNIQUE, un usuario podria inflar la calificacion de un
            // lugar publicando resenas repetidas.
            assertThatThrownBy(() -> insertarResena(1, "Segunda"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        private void insertarResena(int calificacion, String comentario) {
            jdbc.update("""
                    INSERT INTO resena (id, usuario_id, lugar_id, calificacion, comentario, estado)
                    VALUES (uuid_generar_v7(), ?, ?, ?, ?, 'PUBLICADA')
                    """, usuarioId, lugarId, calificacion, comentario);
        }
    }

    @Nested
    @DisplayName("ReporteContenido")
    class ReporteContenidoTest {

        @Test
        @DisplayName("exige exactamente una de las dos claves foraneas")
        void exigeExactamenteUnaFk() {
            UUID resenaId = jdbc.queryForObject("""
                    INSERT INTO resena (id, usuario_id, lugar_id, calificacion, estado)
                    VALUES (uuid_generar_v7(), ?, ?, 4, 'PUBLICADA') RETURNING id
                    """, UUID.class, usuarioId, lugarId);

            // Ninguna de las dos: no se sabe que se esta reportando.
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO reporte_contenido (id, usuario_id, foto_id, resena_id, motivo)
                    VALUES (uuid_generar_v7(), ?, NULL, NULL, 'SPAM')
                    """, usuarioId))
                    .isInstanceOf(DataIntegrityViolationException.class);

            // Con una sola, correcto.
            assertThatCode(() -> jdbc.update("""
                    INSERT INTO reporte_contenido (id, usuario_id, resena_id, motivo)
                    VALUES (uuid_generar_v7(), ?, ?, 'OFENSIVO')
                    """, usuarioId, resenaId))
                    .doesNotThrowAnyException();

            // Y el UNIQUE parcial impide que el mismo usuario cuente dos veces.
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO reporte_contenido (id, usuario_id, resena_id, motivo)
                    VALUES (uuid_generar_v7(), ?, ?, 'FALSO')
                    """, usuarioId, resenaId))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("HorarioLugar")
    class HorarioTest {

        @Test
        @DisplayName("rechaza un dia de la semana fuera de 0-6")
        void rechazaDiaInvalido() {
            assertThatThrownBy(() -> insertarHorario(7, "09:00", "17:00", false))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rechaza una apertura posterior al cierre")
        void rechazaRangoInvertido() {
            assertThatThrownBy(() -> insertarHorario(1, "18:00", "09:00", false))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("permite un dia cerrado sin horas")
        void permiteDiaCerrado() {
            assertThatCode(() -> insertarHorario(0, null, null, true)).doesNotThrowAnyException();
        }

        private void insertarHorario(int dia, String apertura, String cierre, boolean cerrado) {
            jdbc.update("""
                    INSERT INTO horario_lugar (id, lugar_id, dia_semana, hora_apertura, hora_cierre, cerrado)
                    VALUES (uuid_generar_v7(), ?, ?, ?::time, ?::time, ?)
                    """, lugarId, dia, apertura, cierre, cerrado);
        }
    }

    @Nested
    @DisplayName("Restriccion geografica a Ayacucho (RF-22b)")
    class BoundsTest {

        @Test
        @DisplayName("rechaza coordenadas fuera de la region")
        void rechazaCoordenadasFuera() {
            // Plaza de Armas de Lima: valida como punto, invalida para este sistema.
            assertThatThrownBy(() -> insertarLugarEn("fuera-lima", -77.0428, -12.0464))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("acepta coordenadas dentro de la region")
        void aceptaCoordenadasDentro() {
            assertThatCode(() -> insertarLugarEn("dentro-huamanga", -74.2236, -13.1588))
                    .doesNotThrowAnyException();
        }

        private void insertarLugarEn(String slug, double lon, double lat) {
            jdbc.update("""
                    INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, estado)
                    SELECT uuid_generar_v7(), ?, c.id, d.id,
                           ST_SetSRID(ST_MakePoint(?, ?), 4326), 'BORRADOR'
                    FROM categoria_lugar c, distrito d
                    WHERE c.codigo = 'MUSEOS' AND d.codigo = '050101'
                    """, slug, lon, lat);
        }
    }

    @Nested
    @DisplayName("Otras reglas")
    class OtrasReglasTest {

        @Test
        @DisplayName("rechaza un email con formato invalido")
        void rechazaEmailInvalido() {
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO usuario (id, email, password_hash, nombre, rol_id, estado)
                    SELECT uuid_generar_v7(), 'esto-no-es-un-email', 'hash', 'X', r.id, 'ACTIVO'
                    FROM rol r WHERE r.nombre = 'USUARIO'
                    """))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rechaza un anio historico fuera de 1500-1990")
        void rechazaAnioHistoricoInvalido() {
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO lugar_imagen_historica
                        (id, lugar_id, titulo, url_historica, public_id_historica, anio_historico)
                    VALUES (uuid_generar_v7(), ?, 'X', 'http://x', 'x', 1991)
                    """, lugarId))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rechaza un idioma no soportado en las traducciones")
        void rechazaIdiomaNoSoportado() {
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO lugar_traduccion (lugar_id, idioma, nombre)
                    VALUES (?, 'fr', 'Cathedrale')
                    """, lugarId))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("la clave compuesta impide favoritos duplicados")
        void impideFavoritoDuplicado() {
            jdbc.update("INSERT INTO favorito (usuario_id, lugar_id) VALUES (?, ?)", usuarioId, lugarId);
            assertThatThrownBy(() -> jdbc.update(
                    "INSERT INTO favorito (usuario_id, lugar_id) VALUES (?, ?)", usuarioId, lugarId))
                    .isInstanceOf(DataIntegrityViolationException.class);

            Integer total = jdbc.queryForObject("SELECT count(*) FROM favorito", Integer.class);
            assertThat(total).isEqualTo(1);
        }
    }
}
