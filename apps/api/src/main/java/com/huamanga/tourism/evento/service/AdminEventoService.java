package com.huamanga.tourism.evento.service;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.evento.domain.EstadoEvento;
import com.huamanga.tourism.evento.domain.Evento;
import com.huamanga.tourism.evento.domain.EventoTraduccion;
import com.huamanga.tourism.evento.domain.EventoTraduccionId;
import com.huamanga.tourism.evento.dto.ClonarEventoRequest;
import com.huamanga.tourism.evento.dto.EventoDetalleResponse;
import com.huamanga.tourism.evento.dto.EventoRequest;
import com.huamanga.tourism.evento.dto.EventoResumenResponse;
import com.huamanga.tourism.evento.dto.EventoTraduccionRequest;
import com.huamanga.tourism.evento.mapper.EventoMapper;
import com.huamanga.tourism.evento.repository.EventoRepository;
import com.huamanga.tourism.geografia.repository.DistritoRepository;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Gestion de la agenda desde el panel (RF-86).
 *
 * <p>Se separa del {@link EventoService} publico a proposito: la lectura la usa
 * cualquier visitante y la escritura solo un administrador. Tenerlas en clases
 * distintas hace evidente cual es cual y evita que un metodo de escritura acabe
 * colandose en un controlador publico.</p>
 */
@Service
public class AdminEventoService {

    private final EventoRepository eventoRepository;
    private final DistritoRepository distritoRepository;
    private final LugarRepository lugarRepository;
    private final ClimaEventoService climaEventos;
    private final EventoMapper mapper;
    private final Clock clock;

    public AdminEventoService(EventoRepository eventoRepository,
                              DistritoRepository distritoRepository,
                              LugarRepository lugarRepository,
                              ClimaEventoService climaEventos,
                              EventoMapper mapper,
                              Clock clock) {
        this.eventoRepository = eventoRepository;
        this.distritoRepository = distritoRepository;
        this.lugarRepository = lugarRepository;
        this.climaEventos = climaEventos;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Bandeja completa: incluye borradores, cancelados y archivados. */
    @Transactional(readOnly = true)
    public List<EventoResumenResponse> bandeja(Idioma idioma) {
        return eventoRepository.findAllByOrderByFechaInicioDesc().stream()
                .map(evento -> mapper.aResumen(evento, idioma))
                .toList();
    }

    @Transactional
    public EventoDetalleResponse crear(EventoRequest peticion, Idioma idioma) {
        Evento evento = new Evento();
        aplicar(peticion, evento);
        return aDetalle(eventoRepository.save(evento), idioma);
    }

    @Transactional
    public EventoDetalleResponse actualizar(UUID id, EventoRequest peticion, Idioma idioma) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("evento", id.toString()));

        aplicar(peticion, evento);
        return aDetalle(eventoRepository.save(evento), idioma);
    }

    /** Baja logica: la fila se conserva para auditoria (RF-56). */
    @Transactional
    public void eliminar(UUID id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("evento", id.toString()));

        evento.eliminar(clock.instant());
        eventoRepository.save(evento);
    }

    // ---------------------------------------------------------------
    //  Clonado anual (RF-86)
    // ---------------------------------------------------------------

    /**
     * Crea la edicion de otro anio a partir de una festividad recurrente.
     *
     * <p><strong>Lo que se copia es la plantilla, no la fecha.</strong> Nombre,
     * descripcion, organizador, tipo, lugar, distrito y portada describen la
     * fiesta y son los mismos cada anio. La fecha no: la Semana Santa de
     * Ayacucho va atada a la Pascua y se mueve casi un mes de un anio a otro, y
     * el Carnaval igual. Copiar la fecha vieja seria publicar un dato falso
     * precisamente en el evento que mas gente consulta.</p>
     *
     * <p>Por eso pasan tres cosas:</p>
     * <ol>
     *   <li>El clon nace en <strong>BORRADOR</strong>. Nadie ve unas fechas que
     *       una persona no haya confirmado.</li>
     *   <li>Si quien clona indica las fechas reales, se usan esas.</li>
     *   <li>Si no las indica, se <em>proponen</em> desplazando el anio, que es
     *       correcto para las fiestas de fecha fija (el 9 de diciembre lo es) y
     *       un punto de partida editable para las moviles.</li>
     * </ol>
     */
    @Transactional
    public EventoDetalleResponse clonar(UUID id, ClonarEventoRequest peticion, Idioma idioma) {
        Evento original = eventoRepository.findByIdConTraducciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("evento", id.toString()));

        if (!original.isRecurrenteAnual()) {
            throw new EventoNoRecurrenteException(id);
        }

        int anio = peticion.anio();

        if (peticion.fechaInicio() != null && peticion.fechaInicio().getYear() != anio) {
            throw new FechaDeClonIncoherenteException(anio, peticion.fechaInicio().getYear());
        }

        if (eventoRepository.existeClonDe(id, anio)) {
            throw new ClonDuplicadoException(anio);
        }

        Evento clon = new Evento();
        clon.setEventoOrigen(original);
        clon.setTipo(original.getTipo());
        clon.setLugar(original.getLugar());
        clon.setDistrito(original.getDistrito());
        // Se copia la URL, no el binario: es el mismo cartel de la misma fiesta,
        // y duplicar el archivo en Cloudinary gastaria cuota para nada.
        clon.setCloudinaryUrlPortada(original.getCloudinaryUrlPortada());
        clon.setRecurrenteAnual(true);
        clon.setEstado(EstadoEvento.BORRADOR);

        LocalDate inicio = peticion.fechaInicio() != null
                ? peticion.fechaInicio()
                : original.getFechaInicio().plusYears(anio - original.getFechaInicio().getYear());

        // Sin fecha de fin se conserva la DURACION, no se desplaza tambien el
        // anio de la fecha final: al cruzar un bisiesto, desplazar las dos por
        // separado alarga o acorta la fiesta un dia sin que nadie lo advierta.
        LocalDate fin = peticion.fechaFin() != null
                ? peticion.fechaFin()
                : inicio.plusDays(original.duracionEnDias() - 1);

        if (fin.isBefore(inicio)) {
            throw new FechaDeClonIncoherenteException(anio, fin.getYear());
        }

        clon.setFechaInicio(inicio);
        clon.setFechaFin(fin);

        copiarTraducciones(original, clon);

        return aDetalle(eventoRepository.save(clon), idioma);
    }

    /**
     * Copia el texto creando filas nuevas.
     *
     * <p>Reutilizar las instancias del original no seria copiar sino
     * <strong>mover</strong>: la clave primaria de la traduccion es
     * {@code (evento_id, idioma)}, asi que Hibernate reasignaria las del
     * original al clon y la edicion anterior se quedaria sin texto.</p>
     */
    private void copiarTraducciones(Evento original, Evento clon) {
        for (EventoTraduccion fuente : original.getTraducciones()) {
            EventoTraduccion copia = new EventoTraduccion();
            copia.setId(new EventoTraduccionId(clon.getId(), fuente.getId().getIdioma()));
            copia.setEvento(clon);
            copia.setNombre(fuente.getNombre());
            copia.setDescripcion(fuente.getDescripcion());
            copia.setOrganizador(fuente.getOrganizador());
            clon.getTraducciones().add(copia);
        }
    }

    // ---------------------------------------------------------------
    //  Interno
    // ---------------------------------------------------------------

    private void aplicar(EventoRequest peticion, Evento evento) {
        evento.setDistrito(distritoRepository.findById(peticion.distritoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "distrito", peticion.distritoId().toString())));

        evento.setLugar(peticion.lugarId() == null ? null
                : lugarRepository.findById(peticion.lugarId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "lugar", peticion.lugarId().toString())));

        evento.setTipo(peticion.tipo());
        evento.setFechaInicio(peticion.fechaInicio());
        evento.setFechaFin(peticion.fechaFin());
        evento.setCloudinaryUrlPortada(peticion.cloudinaryUrlPortada());
        evento.setRecurrenteAnual(peticion.recurrenteAnual());
        evento.setEstado(peticion.estado());

        reemplazarTraducciones(peticion, evento);
    }

    /**
     * Vacia y rellena la MISMA coleccion.
     *
     * <p>Con {@code orphanRemoval = true}, asignar un Set nuevo hace que
     * Hibernate pierda la coleccion que estaba gestionando. Es la misma trampa
     * documentada en {@code LugarService}.</p>
     */
    private void reemplazarTraducciones(EventoRequest peticion, Evento evento) {
        evento.getTraducciones().clear();
        for (EventoTraduccionRequest fuente : peticion.traducciones()) {
            EventoTraduccion traduccion = new EventoTraduccion();
            traduccion.setId(new EventoTraduccionId(evento.getId(), fuente.idioma()));
            traduccion.setEvento(evento);
            traduccion.setNombre(fuente.nombre());
            traduccion.setDescripcion(fuente.descripcion());
            traduccion.setOrganizador(fuente.organizador());
            evento.getTraducciones().add(traduccion);
        }
    }

    private EventoDetalleResponse aDetalle(Evento evento, Idioma idioma) {
        return mapper.aDetalle(evento, idioma,
                climaEventos.paraEvento(evento.getFechaInicio(), evento.getFechaFin()));
    }

    /** Solo se clonan las festividades marcadas como recurrentes. */
    public static class EventoNoRecurrenteException extends RuntimeException {
        public EventoNoRecurrenteException(UUID id) {
            super("El evento " + id + " no esta marcado como recurrente anual");
        }
    }

    /** Ya hay una edicion clonada de este evento para ese anio. */
    public static class ClonDuplicadoException extends RuntimeException {
        public ClonDuplicadoException(int anio) {
            super("Ya existe un clon de este evento para " + anio);
        }
    }

    /** Las fechas indicadas no corresponden al anio que se pide clonar. */
    public static class FechaDeClonIncoherenteException extends RuntimeException {
        public FechaDeClonIncoherenteException(int anioPedido, int anioFecha) {
            super("Se pidio clonar a " + anioPedido + " pero las fechas son de " + anioFecha);
        }
    }
}
