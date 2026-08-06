package com.huamanga.tourism.resena.service;

import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.evento.ContenidoCalificadoEvent;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import com.huamanga.tourism.resena.domain.EstadoResena;
import com.huamanga.tourism.resena.domain.Resena;
import com.huamanga.tourism.resena.dto.ResenaRequest;
import com.huamanga.tourism.resena.dto.ResenaResponse;
import com.huamanga.tourism.resena.repository.ResenaRepository;
import com.huamanga.tourism.usuario.domain.Usuario;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Reseñas y calificaciones (RF-37).
 *
 * <p><strong>Aqui no se calcula ningun promedio.</strong> El promedio de un
 * lugar sale exclusivamente de la vista materializada
 * {@code estadistica_lugar}; guardarlo en una columna de {@code lugar} seria un
 * atributo derivado y romperia la 3FN que sostiene el modelo. Lo que si hace
 * este servicio es <em>avisar</em> de que los agregados quedaron desfasados.</p>
 */
@Service
public class ResenaService {

    private final ResenaRepository resenaRepository;
    private final LugarRepository lugarRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventos;

    public ResenaService(ResenaRepository resenaRepository,
                         LugarRepository lugarRepository,
                         UsuarioRepository usuarioRepository,
                         ApplicationEventPublisher eventos) {
        this.resenaRepository = resenaRepository;
        this.lugarRepository = lugarRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventos = eventos;
    }

    // ---------------------------------------------------------------
    //  Lectura publica
    // ---------------------------------------------------------------

    /**
     * Reseñas visibles de un lugar.
     *
     * <p>Se leen de la tabla <strong>en vivo</strong>, no de agregados: es lo
     * que permite que quien acaba de opinar vea su reseña al instante aunque la
     * vista materializada tarde en recalcular el promedio.</p>
     */
    @Transactional(readOnly = true)
    public Page<ResenaResponse> listar(String slugLugar, Pageable pagina) {
        Lugar lugar = lugarPublicado(slugLugar);
        return resenaRepository
                .findByLugarIdAndEstadoConAutor(lugar.getId(), EstadoResena.PUBLICADA, pagina)
                .map(this::aRespuesta);
    }

    /**
     * La reseña que dejo el usuario actual, para poder editarla.
     *
     * <p>Una reseña borrada <strong>no cuenta</strong>: la fila sigue ahi por
     * la baja logica, pero para quien la escribio ya no existe y debe poder
     * escribir otra.</p>
     */
    @Transactional(readOnly = true)
    public ResenaResponse mia(String slugLugar) {
        Lugar lugar = lugarPublicado(slugLugar);
        return resenaRepository
                .findByUsuarioIdAndLugarId(UsuarioActual.idObligatorio(), lugar.getId())
                .filter(resena -> resena.getEstado() != EstadoResena.ELIMINADA)
                .map(this::aRespuesta)
                .orElse(null);
    }

    // ---------------------------------------------------------------
    //  Escritura
    // ---------------------------------------------------------------

    @Transactional
    public ResenaResponse crear(String slugLugar, ResenaRequest peticion) {
        Lugar lugar = lugarPublicado(slugLugar);
        UUID usuarioId = UsuarioActual.idObligatorio();

        // El UNIQUE (usuario, lugar) de la base es la garantia real; esta
        // comprobacion existe para responder un 409 con un mensaje util en vez
        // de un error de integridad indescifrable.
        var existente = resenaRepository.findByUsuarioIdAndLugarId(usuarioId, lugar.getId());

        if (existente.isPresent()) {
            Resena anterior = existente.get();

            // Si la borro, se REUTILIZA su fila en vez de rechazar la nueva.
            // El UNIQUE impide crear otra, asi que sin esto quien borrara su
            // resena no podria volver a opinar en ese lugar nunca mas: la
            // baja logica se convertiria en una condena permanente.
            if (anterior.getEstado() == EstadoResena.ELIMINADA) {
                anterior.setCalificacion(peticion.calificacion());
                anterior.setComentario(peticion.comentarioNormalizado());
                anterior.setEstado(EstadoResena.PUBLICADA);

                Resena revivida = resenaRepository.save(anterior);
                avisarCambioDeCalificacion(lugar);
                return aRespuesta(revivida);
            }

            throw new ResenaDuplicadaException(slugLugar);
        }

        Usuario autor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario", usuarioId.toString()));

        Resena resena = new Resena();
        resena.setUsuario(autor);
        resena.setLugar(lugar);
        resena.setCalificacion(peticion.calificacion());
        resena.setComentario(peticion.comentarioNormalizado());
        resena.setEstado(EstadoResena.PUBLICADA);

        Resena guardada = resenaRepository.save(resena);
        avisarCambioDeCalificacion(lugar);

        return aRespuesta(guardada);
    }

    /**
     * Edita la propia reseña.
     *
     * <p>Dos reglas que no son obvias:</p>
     * <ul>
     *   <li>Solo el autor puede editarla. Se comprueba <strong>aqui</strong> y
     *       no en el controller, porque es una regla de negocio y debe valer
     *       venga la llamada de donde venga.</li>
     *   <li>Editar una reseña que un administrador oculto <strong>no la
     *       republica</strong>. Si bastara con editarla para volver a verse, la
     *       moderacion no serviria de nada.</li>
     * </ul>
     */
    @Transactional
    public ResenaResponse editar(UUID resenaId, ResenaRequest peticion) {
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("resena", resenaId.toString()));

        if (!resena.getUsuario().getId().equals(UsuarioActual.idObligatorio())) {
            throw new ResenaAjenaException();
        }

        resena.setCalificacion(peticion.calificacion());
        resena.setComentario(peticion.comentarioNormalizado());
        // El estado se deja intacto a proposito: ver el javadoc.

        Resena guardada = resenaRepository.save(resena);
        avisarCambioDeCalificacion(resena.getLugar());

        return aRespuesta(guardada);
    }

    /**
     * Baja logica de la propia reseña.
     *
     * <p>La fila se conserva con estado {@code ELIMINADA}: deja rastro para
     * auditoria y para poder revertir un borrado por error, y al no estar
     * {@code PUBLICADA} deja de contar en el promedio.</p>
     */
    @Transactional
    public void eliminar(UUID resenaId) {
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("resena", resenaId.toString()));

        if (!resena.getUsuario().getId().equals(UsuarioActual.idObligatorio())) {
            throw new ResenaAjenaException();
        }

        resena.setEstado(EstadoResena.ELIMINADA);
        resenaRepository.save(resena);
        avisarCambioDeCalificacion(resena.getLugar());
    }

    // ---------------------------------------------------------------
    //  Interno
    // ---------------------------------------------------------------

    /**
     * Avisa de que los agregados del lugar quedaron desfasados.
     *
     * <p>El evento se consume DESPUES del commit. Si se consumiera antes, el
     * refresco leeria la reseña todavia sin confirmar —o peor, una que luego se
     * revierte— y el promedio quedaria mal hasta el siguiente ciclo.</p>
     */
    private void avisarCambioDeCalificacion(Lugar lugar) {
        eventos.publishEvent(new ContenidoCalificadoEvent(lugar.getId(), lugar.getSlug()));
    }

    private Lugar lugarPublicado(String slug) {
        return lugarRepository.findBySlug(slug)
                .filter(l -> l.getEstado() == EstadoLugar.PUBLICADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("lugar", slug));
    }

    private ResenaResponse aRespuesta(Resena resena) {
        // Se considera editada si se modifico despues de crearse. Un segundo de
        // margen absorbe la diferencia natural entre ambos sellos al insertar.
        boolean editada = resena.getUpdatedAt() != null
                && resena.getCreatedAt() != null
                && resena.getUpdatedAt().isAfter(resena.getCreatedAt().plusSeconds(1));

        return new ResenaResponse(
                resena.getId(),
                resena.getCalificacion(),
                resena.getComentario(),
                resena.getUsuario().getNombre(),
                resena.getUsuario().getId(),
                resena.getEstado(),
                resena.getCreatedAt(),
                editada);
    }

    /** Un usuario solo puede opinar una vez por lugar (RF-37). */
    public static class ResenaDuplicadaException extends RuntimeException {
        public ResenaDuplicadaException(String slug) {
            super("Ya dejaste una resena en " + slug);
        }
    }

    /** Editar o borrar la reseña de otra persona. */
    public static class ResenaAjenaException extends RuntimeException {
        public ResenaAjenaException() {
            super("Solo puedes modificar tu propia resena");
        }
    }
}
