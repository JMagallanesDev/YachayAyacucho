package com.huamanga.tourism.seguridad;

import com.huamanga.tourism.geografia.repository.DistritoRepository;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.repository.CategoriaLugarRepository;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import com.huamanga.tourism.soporte.BasePostgis;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Auditoria automatica: {@code created_by} y {@code updated_by}.
 *
 * <p>Cierra el segundo cabo del Bloque 1. Con el {@code AuditorAware}
 * leyendo el SecurityContext, las 6 entidades auditables registran quien las
 * creo sin que ningun service tenga que acordarse de asignarlo, que es la
 * unica forma de que la auditoria no se olvide nunca (RF-56).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Auditoria automatica desde el SecurityContext")
class AuditoriaAutomaticaTest extends BasePostgis {

    private static final GeometryFactory GEOMETRIAS =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private CategoriaLugarRepository categoriaRepository;

    @Autowired
    private DistritoRepository distritoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.execute("DELETE FROM lugar");
        // La bitacora de administracion (RF-56, Bloque 10) referencia al
        // usuario, asi que hay que vaciarla antes de borrar cuentas.
        jdbc.execute("DELETE FROM registro_actividad");
        jdbc.execute("DELETE FROM usuario");
        SecurityContextHolder.clearContext();
    }

    /**
     * Crea un usuario real en la base.
     *
     * <p>Hace falta uno de verdad porque {@code created_by} y
     * {@code updated_by} son claves foraneas hacia {@code usuario}: un UUID
     * inventado violaria la integridad referencial, que es exactamente lo que
     * esas FK estan ahi para impedir.</p>
     */
    private UUID crearUsuario(String email) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (id, email, password_hash, nombre, rol_id, estado)
                SELECT uuid_generar_v7(), ?, 'hash', 'Persona', r.id, 'ACTIVO'
                FROM rol r WHERE r.nombre = 'ADMIN' RETURNING id
                """, UUID.class, email);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("rellena created_by con el usuario autenticado")
    void rellenaCreatedByConElUsuarioAutenticado() {
        UUID usuarioId = crearUsuario("editor@yachay.pe");
        autenticarComo(usuarioId);

        Lugar guardado = lugarRepository.saveAndFlush(nuevoLugar("auditado"));

        assertThat(guardado.getCreatedBy()).isEqualTo(usuarioId);
        assertThat(guardado.getUpdatedBy()).isEqualTo(usuarioId);
    }

    @Test
    @DisplayName("deja created_by a null en operaciones anonimas")
    void dejaCreatedByNuloSinAutenticacion() {
        // Es lo correcto: un reporte ciudadano anonimo o un job programado no
        // tienen a quien atribuirse, y falsear un autor seria peor que el null.
        Lugar guardado = lugarRepository.saveAndFlush(nuevoLugar("anonimo"));

        assertThat(guardado.getCreatedBy()).isNull();
    }

    @Test
    @DisplayName("actualiza updated_by al modificar con otro usuario")
    void actualizaUpdatedByAlModificar() {
        UUID creador = crearUsuario("creador@yachay.pe");
        autenticarComo(creador);
        Lugar lugar = lugarRepository.saveAndFlush(nuevoLugar("editado"));

        UUID editor = crearUsuario("revisor@yachay.pe");
        autenticarComo(editor);
        lugar.setDireccion("Nueva direccion verificada");
        Lugar actualizado = lugarRepository.saveAndFlush(lugar);

        // Queda constancia de quien lo creo y de quien lo toco por ultima vez.
        assertThat(actualizado.getCreatedBy()).isEqualTo(creador);
        assertThat(actualizado.getUpdatedBy()).isEqualTo(editor);
    }

    private void autenticarComo(UUID usuarioId) {
        Jwt jwt = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "HS256")
                .subject(usuarioId.toString())
                .claim("rol", "ADMIN")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }

    private Lugar nuevoLugar(String slug) {
        Lugar lugar = new Lugar();
        lugar.setSlug(slug);
        lugar.setCategoria(categoriaRepository.findByCodigo("IGLESIAS").orElseThrow());
        lugar.setDistrito(distritoRepository.findByCodigo("050101").orElseThrow());
        Point punto = GEOMETRIAS.createPoint(new Coordinate(-74.2236, -13.1588));
        punto.setSRID(4326);
        lugar.setUbicacion(punto);
        lugar.setEstado(EstadoLugar.BORRADOR);
        return lugar;
    }
}
