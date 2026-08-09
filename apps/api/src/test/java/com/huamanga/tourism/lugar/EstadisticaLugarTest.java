package com.huamanga.tourism.lugar;

import com.huamanga.tourism.lugar.domain.EstadisticaLugar;
import com.huamanga.tourism.lugar.repository.EstadisticaLugarRepository;
import com.huamanga.tourism.lugar.service.EstadisticaLugarService;
import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vista materializada de agregados por lugar (seccion 6.3 del plan).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Vista materializada EstadisticaLugar")
class EstadisticaLugarTest extends BasePostgis {

    @Autowired
    private EstadisticaLugarService estadisticaLugarService;

    @Autowired
    private EstadisticaLugarRepository estadisticaRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID lugarId;

    @BeforeEach
    void prepararDatos() {
        jdbc.execute("DELETE FROM resena");
        jdbc.execute("DELETE FROM check_in");
        jdbc.execute("DELETE FROM favorito");
        jdbc.execute("DELETE FROM lugar");
        // La bitacora de administracion (RF-56, Bloque 10) referencia al
        // usuario, asi que hay que vaciarla antes de borrar cuentas.
        jdbc.execute("DELETE FROM registro_actividad");
        jdbc.execute("DELETE FROM usuario");

        lugarId = jdbc.queryForObject("""
                INSERT INTO lugar (id, slug, categoria_lugar_id, distrito_id, ubicacion, estado)
                SELECT uuid_generar_v7(), 'lugar-estadistica', c.id, d.id,
                       ST_SetSRID(ST_MakePoint(-74.22, -13.16), 4326), 'PUBLICADO'
                FROM categoria_lugar c, distrito d
                WHERE c.codigo = 'IGLESIAS' AND d.codigo = '050101' RETURNING id
                """, UUID.class);
    }

    @Test
    @DisplayName("promedia solo las resenas publicadas, no las ocultas por moderacion")
    void promediaSoloResenasPublicadas() {
        crearUsuarioConResena("a@yachay.pe", 5, "PUBLICADA");
        crearUsuarioConResena("b@yachay.pe", 3, "PUBLICADA");
        // Una resena de 1 estrella oculta por moderacion no debe hundir el
        // promedio publico del lugar.
        crearUsuarioConResena("c@yachay.pe", 1, "OCULTA");

        estadisticaLugarService.refrescar();

        EstadisticaLugar estadistica = estadisticaRepository.findById(lugarId).orElseThrow();
        assertThat(estadistica.getCalificacionPromedio()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(estadistica.getTotalResenas()).isEqualTo(2);
    }

    @Test
    @DisplayName("un lugar sin actividad aparece con contadores en cero, no ausente")
    void lugarSinActividadApareceEnCero() {
        estadisticaLugarService.refrescar();

        EstadisticaLugar estadistica = estadisticaRepository.findById(lugarId).orElseThrow();

        assertThat(estadistica.getCalificacionPromedio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(estadistica.getTotalResenas()).isZero();
        assertThat(estadistica.getTotalVisitas()).isZero();
        assertThat(estadistica.getTotalFavoritos()).isZero();
    }

    @Test
    @DisplayName("cuenta check-ins y favoritos por lugar")
    void cuentaVisitasYFavoritos() {
        UUID usuarioId = crearUsuario("visitante@yachay.pe");
        jdbc.update("""
                INSERT INTO check_in (id, usuario_id, lugar_id, ubicacion_gps)
                VALUES (uuid_generar_v7(), ?, ?, ST_SetSRID(ST_MakePoint(-74.22, -13.16), 4326))
                """, usuarioId, lugarId);
        jdbc.update("INSERT INTO favorito (usuario_id, lugar_id) VALUES (?, ?)", usuarioId, lugarId);

        estadisticaLugarService.refrescar();

        EstadisticaLugar estadistica = estadisticaRepository.findById(lugarId).orElseThrow();
        assertThat(estadistica.getTotalVisitas()).isEqualTo(1);
        assertThat(estadistica.getTotalFavoritos()).isEqualTo(1);
    }

    @Test
    @DisplayName("los lugares eliminados desaparecen de la vista")
    void excluyeLugaresEliminados() {
        jdbc.update("UPDATE lugar SET deleted_at = NOW() WHERE id = ?", lugarId);

        estadisticaLugarService.refrescar();

        assertThat(estadisticaRepository.findById(lugarId)).isEmpty();
    }

    @Test
    @DisplayName("REFRESH CONCURRENTLY funciona y depende del indice unico")
    void refrescoConcurrenteExigeIndiceUnico() {
        // El refresco concurrente es lo que permite que el ranking siga
        // consultable mientras se recalcula.
        assertThatCode(() -> estadisticaLugarService.refrescar()).doesNotThrowAnyException();

        // Sin el indice unico PostgreSQL lo rechaza: se comprueba sobre una
        // vista gemela sin indice para no tocar la real.
        jdbc.execute("CREATE MATERIALIZED VIEW vista_sin_indice AS SELECT id FROM lugar");
        try {
            assertThatThrownBy(() ->
                    jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY vista_sin_indice"))
                    .hasMessageContaining("unique index");
        } finally {
            jdbc.execute("DROP MATERIALIZED VIEW vista_sin_indice");
        }
    }

    private UUID crearUsuario(String email) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (id, email, password_hash, nombre, rol_id, estado)
                SELECT uuid_generar_v7(), ?, 'hash', 'Usuario', r.id, 'ACTIVO'
                FROM rol r WHERE r.nombre = 'USUARIO' RETURNING id
                """, UUID.class, email);
    }

    private void crearUsuarioConResena(String email, int calificacion, String estado) {
        UUID usuarioId = crearUsuario(email);
        jdbc.update("""
                INSERT INTO resena (id, usuario_id, lugar_id, calificacion, estado)
                VALUES (uuid_generar_v7(), ?, ?, ?, ?)
                """, usuarioId, lugarId, calificacion, estado);
    }
}
