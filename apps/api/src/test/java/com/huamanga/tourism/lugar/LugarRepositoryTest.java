package com.huamanga.tourism.lugar;

import com.huamanga.tourism.geografia.repository.DistritoRepository;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.repository.CategoriaLugarRepository;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import com.huamanga.tourism.soporte.BasePostgis;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repositorio de lugares contra PostgreSQL + PostGIS reales.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("LugarRepository")
class LugarRepositoryTest extends BasePostgis {

    /** SRID 4326 = WGS84, el sistema de coordenadas del GPS. */
    private static final GeometryFactory GEOMETRIAS =
            new GeometryFactory(new PrecisionModel(), 4326);

    /** Plaza Mayor de Huamanga. */
    private static final double LON_PLAZA = -74.2236;
    private static final double LAT_PLAZA = -13.1588;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private CategoriaLugarRepository categoriaRepository;

    @Autowired
    private DistritoRepository distritoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void limpiar() {
        jdbc.execute("DELETE FROM lugar");
    }

    @Test
    @DisplayName("guarda y recupera un punto geografico sin perder precision")
    void guardaYRecuperaGeometria() {
        Lugar guardado = lugarRepository.saveAndFlush(nuevoLugar("catedral", LON_PLAZA, LAT_PLAZA));

        Lugar recuperado = lugarRepository.findBySlug("catedral").orElseThrow();

        assertThat(recuperado.getUbicacion().getX()).isEqualTo(LON_PLAZA);
        assertThat(recuperado.getUbicacion().getY()).isEqualTo(LAT_PLAZA);
        assertThat(recuperado.getUbicacion().getSRID()).isEqualTo(4326);
        assertThat(recuperado.getId()).isEqualTo(guardado.getId());
    }

    @Test
    @DisplayName("asigna un UUID v7 al persistir, no antes")
    void asignaUuidV7AlPersistir() {
        Lugar sinGuardar = nuevoLugar("sin-guardar", LON_PLAZA, LAT_PLAZA);

        // Antes de persistir no hay id: es lo que permite a Spring Data
        // distinguir una entidad nueva y hacer INSERT sin un SELECT previo.
        assertThat(sinGuardar.getId()).isNull();

        Lugar guardado = lugarRepository.saveAndFlush(sinGuardar);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getId().version()).isEqualTo(7);
        assertThat(guardado.getCreatedAt()).isNotNull();
        assertThat(guardado.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("encuentra lugares dentro de un radio en metros y los ordena por cercania")
    void buscaPorCercania() {
        lugarRepository.saveAndFlush(nuevoLugar("en-la-plaza", LON_PLAZA, LAT_PLAZA));
        // ~330 m al sur
        lugarRepository.saveAndFlush(nuevoLugar("a-300-metros", LON_PLAZA, LAT_PLAZA - 0.003));
        // ~5,5 km al este
        lugarRepository.saveAndFlush(nuevoLugar("a-5-km", LON_PLAZA + 0.05, LAT_PLAZA));
        lugarRepository.flush();

        List<Lugar> cercanos = lugarRepository.buscarCercaDe(LON_PLAZA, LAT_PLAZA, 1000);

        assertThat(cercanos).extracting(Lugar::getSlug)
                .containsExactly("en-la-plaza", "a-300-metros");
    }

    @Test
    @DisplayName("calcula la distancia en metros a un punto (RF-09c)")
    void calculaDistanciaEnMetros() {
        Lugar lugar = lugarRepository.saveAndFlush(
                nuevoLugar("a-un-km", LON_PLAZA, LAT_PLAZA - 0.009));
        lugarRepository.flush();

        Double metros = lugarRepository.distanciaEnMetros(lugar.getId(), LON_PLAZA, LAT_PLAZA);

        // 0,009 grados de latitud son aproximadamente 1 km.
        assertThat(metros).isBetween(900.0, 1100.0);
    }

    @Test
    @DisplayName("el soft delete oculta el lugar sin borrar la fila")
    void softDeleteOcultaSinBorrar() {
        Lugar lugar = lugarRepository.saveAndFlush(nuevoLugar("por-eliminar", LON_PLAZA, LAT_PLAZA));
        UUID id = lugar.getId();

        lugar.eliminar(Instant.now());
        lugarRepository.saveAndFlush(lugar);

        // Vaciar el contexto de persistencia es imprescindible: sin esto,
        // findById devolveria la instancia que sigue en la cache de primer
        // nivel sin llegar a consultar la base, y el test pasaria sin probar
        // que el filtro de soft delete funciona.
        entityManager.clear();

        // Para la aplicacion ya no existe...
        assertThat(lugarRepository.findById(id)).isEmpty();
        assertThat(lugarRepository.findBySlug("por-eliminar")).isEmpty();

        // ...pero la fila sigue ahi, con su marca de eliminacion, disponible
        // para auditoria o para revertir un error de moderacion.
        Integer filas = jdbc.queryForObject(
                "SELECT count(*) FROM lugar WHERE id = ? AND deleted_at IS NOT NULL", Integer.class, id);
        assertThat(filas).isEqualTo(1);
    }

    private Lugar nuevoLugar(String slug, double longitud, double latitud) {
        Lugar lugar = new Lugar();
        lugar.setSlug(slug);
        lugar.setCategoria(categoriaRepository.findByCodigo("IGLESIAS").orElseThrow());
        lugar.setDistrito(distritoRepository.findByCodigo("050101").orElseThrow());
        lugar.setUbicacion(punto(longitud, latitud));
        lugar.setEstado(EstadoLugar.PUBLICADO);
        return lugar;
    }

    private Point punto(double longitud, double latitud) {
        // Orden (x, y) = (longitud, latitud): invertirlo es el error clasico
        // en geoespacial y colocaria Huamanga en mitad del oceano.
        Point punto = GEOMETRIAS.createPoint(new Coordinate(longitud, latitud));
        punto.setSRID(4326);
        return punto;
    }
}
