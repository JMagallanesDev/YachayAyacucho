package com.huamanga.tourism.ruta.service;

import com.huamanga.tourism.admin.service.RegistroActividadService;
import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import com.huamanga.tourism.ruta.domain.LugarRuta;
import com.huamanga.tourism.ruta.domain.LugarRutaId;
import com.huamanga.tourism.ruta.domain.RutaTematica;
import com.huamanga.tourism.ruta.domain.RutaTraduccion;
import com.huamanga.tourism.ruta.domain.RutaTraduccionId;
import com.huamanga.tourism.ruta.dto.RutaRequest;
import com.huamanga.tourism.ruta.dto.RutaResponse;
import com.huamanga.tourism.ruta.repository.RutaTematicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gestion de rutas tematicas desde el panel (RF-53).
 *
 * <p>El dato con valor de una ruta es <strong>el orden de sus paradas</strong>:
 * una ruta cultural es una secuencia, no un conjunto, y recorrerla en otro orden
 * dibuja un garabato sobre la ciudad. Por eso las paradas llegan como una lista
 * ordenada de identificadores y el servidor las numera de 1 a N: asi no hay forma
 * de que lleguen dos con el mismo numero ni con huecos.</p>
 */
@Service
public class AdminRutaService {

    private final RutaTematicaRepository rutaRepository;
    private final LugarRepository lugarRepository;
    private final RutaService rutaService;
    private final RegistroActividadService auditoria;
    private final Clock clock;

    public AdminRutaService(RutaTematicaRepository rutaRepository,
                            LugarRepository lugarRepository,
                            RutaService rutaService,
                            RegistroActividadService auditoria,
                            Clock clock) {
        this.rutaRepository = rutaRepository;
        this.lugarRepository = lugarRepository;
        this.rutaService = rutaService;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /** Bandeja del panel: incluye las rutas desactivadas. */
    @Transactional(readOnly = true)
    public List<RutaResponse> bandeja(Idioma idioma) {
        return rutaRepository.findTodasConRecorrido().stream()
                .map(ruta -> rutaService.aRespuestaPublica(ruta, idioma))
                .toList();
    }

    @Transactional
    public RutaResponse crear(RutaRequest peticion, Idioma idioma) {
        if (rutaRepository.existsBySlug(peticion.slug())) {
            throw new SlugDeRutaDuplicadoException(peticion.slug());
        }

        RutaTematica ruta = new RutaTematica();
        aplicar(peticion, ruta);
        RutaTematica guardada = rutaRepository.save(ruta);

        auditoria.registrar("CREAR_RUTA", "RutaTematica", guardada.getId(),
                Map.of("slug", guardada.getSlug(), "paradas", String.valueOf(peticion.paradas().size())));

        return rutaService.aRespuestaPublica(guardada, idioma);
    }

    @Transactional
    public RutaResponse actualizar(UUID id, RutaRequest peticion, Idioma idioma) {
        RutaTematica ruta = rutaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ruta", id.toString()));

        if (!ruta.getSlug().equals(peticion.slug()) && rutaRepository.existsBySlug(peticion.slug())) {
            throw new SlugDeRutaDuplicadoException(peticion.slug());
        }

        aplicar(peticion, ruta);
        RutaTematica guardada = rutaRepository.save(ruta);

        auditoria.registrar("EDITAR_RUTA", "RutaTematica", id,
                Map.of("slug", guardada.getSlug()));

        return rutaService.aRespuestaPublica(guardada, idioma);
    }

    /** Baja logica: la ruta desaparece del mapa pero la fila se conserva. */
    @Transactional
    public void eliminar(UUID id) {
        RutaTematica ruta = rutaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ruta", id.toString()));

        ruta.eliminar(clock.instant());
        rutaRepository.save(ruta);

        auditoria.registrar("ELIMINAR_RUTA", "RutaTematica", id, Map.of("slug", ruta.getSlug()));
    }

    // ---------------------------------------------------------------
    //  Interno
    // ---------------------------------------------------------------

    private void aplicar(RutaRequest peticion, RutaTematica ruta) {
        ruta.setSlug(peticion.slug());
        ruta.setColorHex(peticion.colorHex());
        ruta.setIcono(peticion.icono());
        ruta.setActiva(peticion.activa());
        ruta.setOrden(peticion.orden() == null ? 0 : peticion.orden());

        reemplazarTraducciones(peticion, ruta);
        reemplazarParadas(peticion, ruta);
    }

    /**
     * Vacia y rellena la MISMA coleccion.
     *
     * <p>Con {@code orphanRemoval = true}, asignar un Set nuevo hace que
     * Hibernate pierda la coleccion que gestionaba. Es la misma trampa
     * documentada en {@code LugarService} y {@code AdminEventoService}.</p>
     */
    private void reemplazarTraducciones(RutaRequest peticion, RutaTematica ruta) {
        ruta.getTraducciones().clear();
        for (RutaRequest.RutaTraduccionRequest fuente : peticion.traducciones()) {
            RutaTraduccion traduccion = new RutaTraduccion();
            traduccion.setId(new RutaTraduccionId(ruta.getId(), fuente.idioma()));
            traduccion.setRuta(ruta);
            traduccion.setNombre(fuente.nombre());
            traduccion.setDescripcion(fuente.descripcion());
            ruta.getTraducciones().add(traduccion);
        }
    }

    /**
     * Numera las paradas por su posicion en la lista.
     *
     * <p>Se rechaza un lugar repetido: pasar dos veces por el mismo sitio no es
     * una ruta con seis paradas sino un error de edicion, y ademas la clave
     * primaria {@code (ruta_id, lugar_id)} lo impediria con un fallo de
     * integridad en vez de con un mensaje comprensible.</p>
     */
    private void reemplazarParadas(RutaRequest peticion, RutaTematica ruta) {
        Set<UUID> vistos = new HashSet<>();
        ruta.getLugares().clear();

        short posicion = 1;
        for (UUID lugarId : peticion.paradas()) {
            if (!vistos.add(lugarId)) {
                throw new ParadaRepetidaException(lugarId);
            }

            Lugar lugar = lugarRepository.findById(lugarId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("lugar", lugarId.toString()));

            LugarRuta parada = new LugarRuta();
            parada.setId(new LugarRutaId(ruta.getId(), lugarId));
            parada.setRuta(ruta);
            parada.setLugar(lugar);
            parada.setOrden(posicion++);
            ruta.getLugares().add(parada);
        }
    }

    /** El slug forma parte de la URL publica de la ruta. */
    public static class SlugDeRutaDuplicadoException extends RuntimeException {
        public SlugDeRutaDuplicadoException(String slug) {
            super("Ya existe una ruta con el slug " + slug);
        }
    }

    /** Una ruta no puede pasar dos veces por el mismo lugar. */
    public static class ParadaRepetidaException extends RuntimeException {
        public ParadaRepetidaException(UUID lugarId) {
            super("El lugar " + lugarId + " aparece dos veces en el recorrido");
        }
    }
}
