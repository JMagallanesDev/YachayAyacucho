package com.huamanga.tourism.checkin.service;

import com.huamanga.tourism.checkin.domain.CheckIn;
import com.huamanga.tourism.checkin.dto.CheckInRequest;
import com.huamanga.tourism.checkin.dto.CheckInResponse;
import com.huamanga.tourism.checkin.repository.CheckInRepository;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.insignia.domain.Insignia;
import com.huamanga.tourism.insignia.service.MotorInsignias;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.evento.ContenidoCalificadoEvent;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Check-in por GPS y sellos del pasaporte (RF-39).
 *
 * <p>Vease {@link ValidadorProximidad} para el encuadre honesto de lo que este
 * mecanismo garantiza y lo que no.</p>
 */
@Service
public class CheckInService {

    /** SRID 4326 = WGS84, el sistema del GPS. */
    private static final GeometryFactory GEOMETRIAS =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final CheckInRepository checkInRepository;
    private final LugarRepository lugarRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidadorProximidad validador;
    private final MotorInsignias motorInsignias;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public CheckInService(CheckInRepository checkInRepository,
                          LugarRepository lugarRepository,
                          UsuarioRepository usuarioRepository,
                          ValidadorProximidad validador,
                          MotorInsignias motorInsignias,
                          JdbcTemplate jdbc,
                          ApplicationEventPublisher eventos,
                          Clock clock) {
        this.checkInRepository = checkInRepository;
        this.lugarRepository = lugarRepository;
        this.usuarioRepository = usuarioRepository;
        this.validador = validador;
        this.motorInsignias = motorInsignias;
        this.jdbc = jdbc;
        this.eventos = eventos;
        this.clock = clock;
    }

    /**
     * Registra una visita si la posicion enviada es plausible.
     *
     * <p>Todo ocurre en <strong>una sola transaccion</strong>: la visita, la
     * evaluacion de insignias y su concesion. Es lo que impide que una caida a
     * mitad de camino deje el sello puesto y la insignia perdida.</p>
     */
    @Transactional
    public CheckInResponse registrar(String slugLugar, CheckInRequest peticion) {
        UUID usuarioId = UsuarioActual.idObligatorio();
        Instant ahora = clock.instant();

        Lugar lugar = lugarRepository.findBySlug(slugLugar)
                .filter(l -> l.getEstado() == EstadoLugar.PUBLICADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("lugar", slugLugar));

        // Enfriamiento primero: es la comprobacion mas barata y evita gastar
        // una consulta espacial en algo que se va a rechazar igualmente.
        if (checkInRepository.existsByUsuarioIdAndLugarIdAndCreatedAtAfter(
                usuarioId, lugar.getId(), ahora.minus(ValidadorProximidad.ENFRIAMIENTO))) {
            throw new ValidadorProximidad.YaHizoCheckInException();
        }

        Point punto = punto(peticion.longitud(), peticion.latitud());

        // La distancia la calcula PostGIS sobre `geography`: metros reales
        // sobre el elipsoide, no grados. Nunca se acepta una distancia
        // calculada por el cliente, que es justo lo que se esta verificando.
        double distancia = distanciaMetros(lugar.getId(), peticion.longitud(), peticion.latitud());

        validador.validar(lugar, distancia, peticion.precision(),
                checkInRepository.ultimoDe(usuarioId), punto, ahora);

        CheckIn visita = new CheckIn();
        visita.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        visita.setLugar(lugar);
        // Se guarda el punto que envio el cliente, no el del lugar: es lo que
        // permite auditar despues un patron sospechoso.
        visita.setUbicacionGps(punto);
        checkInRepository.save(visita);

        // Las visitas alimentan «mas visitados», que sale de la vista
        // materializada; sin este aviso el ranking tardaria hasta 5 minutos.
        eventos.publishEvent(new ContenidoCalificadoEvent(lugar.getId(), lugar.getSlug()));

        List<Insignia> nuevas = motorInsignias.evaluar(usuarioId);

        return new CheckInResponse(
                visita.getId(),
                lugar.getSlug(),
                Math.round(distancia),
                checkInRepository.contarLugaresDistintos(usuarioId),
                nuevas.stream().map(Insignia::getCodigo).toList());
    }

    /**
     * Distancia en metros entre el lugar y el punto enviado.
     *
     * <p>Se usa SQL directo y no JPQL porque {@code ST_Distance} sobre
     * {@code geography} no tiene equivalente portable en JPQL, y traducirlo a
     * una funcion registrada de Hibernate anadiria complejidad sin ganar nada.
     * Los parametros van enlazados, nunca concatenados.</p>
     */
    private double distanciaMetros(UUID lugarId, double longitud, double latitud) {
        Double metros = jdbc.queryForObject("""
                SELECT ST_Distance(
                           l.ubicacion::geography,
                           ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography)
                FROM lugar l WHERE l.id = ?
                """, Double.class, longitud, latitud, lugarId);

        return metros != null ? metros : Double.MAX_VALUE;
    }

    private Point punto(double longitud, double latitud) {
        // Orden (x, y) = (longitud, latitud). Invertirlo es el error clasico en
        // geoespacial y colocaria a cualquiera en mitad del oceano.
        Point punto = GEOMETRIAS.createPoint(new Coordinate(longitud, latitud));
        punto.setSRID(4326);
        return punto;
    }
}
