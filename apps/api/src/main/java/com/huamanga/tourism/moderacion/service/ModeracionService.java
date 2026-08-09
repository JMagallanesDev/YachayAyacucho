package com.huamanga.tourism.moderacion.service;

import com.huamanga.tourism.admin.service.RegistroActividadService;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.foto.domain.EstadoFoto;
import com.huamanga.tourism.foto.domain.Foto;
import com.huamanga.tourism.foto.repository.FotoRepository;
import com.huamanga.tourism.foto.service.ClienteCloudinary;
import com.huamanga.tourism.lugar.evento.ContenidoCalificadoEvent;
import com.huamanga.tourism.lugar.evento.LugarGuardadoEvent;
import com.huamanga.tourism.moderacion.dto.FotoModeracionResponse;
import com.huamanga.tourism.moderacion.dto.ResenaModeracionResponse;
import com.huamanga.tourism.resena.domain.EstadoResena;
import com.huamanga.tourism.resena.domain.Resena;
import com.huamanga.tourism.resena.repository.ResenaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bandejas de moderacion (RF-49, RF-50).
 *
 * <p>Todo lo de aqui exige rol ADMIN, comprobado con {@code @PreAuthorize} en
 * el controller. Esta clase asume que quien llega ya tiene permiso.</p>
 */
@Service
public class ModeracionService {

    private static final Logger log = LoggerFactory.getLogger(ModeracionService.class);

    private static final int TAMANO_BANDEJA = 50;

    private final FotoRepository fotoRepository;
    private final ResenaRepository resenaRepository;
    private final ClienteCloudinary cloudinary;
    private final ApplicationEventPublisher eventos;
    private final RegistroActividadService auditoria;

    public ModeracionService(FotoRepository fotoRepository,
                             ResenaRepository resenaRepository,
                             ClienteCloudinary cloudinary,
                             ApplicationEventPublisher eventos,
                             RegistroActividadService auditoria) {
        this.fotoRepository = fotoRepository;
        this.resenaRepository = resenaRepository;
        this.cloudinary = cloudinary;
        this.eventos = eventos;
        this.auditoria = auditoria;
    }

    // ---------------------------------------------------------------
    //  Fotos (RF-49)
    // ---------------------------------------------------------------

    /**
     * Cola de fotos por revisar.
     *
     * <p>Incluye {@code EN_REVISION} ademas de {@code PENDIENTE} desde el Bloque
     * 10. Una foto llega al primer estado por acumular tres denuncias (RF-45), y
     * hasta ahora eso la sacaba de la galeria publica <strong>sin meterla en
     * ninguna cola</strong>: quedaba en tierra de nadie, invisible tambien para
     * quien debia juzgarla. El {@code estado} viaja en la respuesta para que el
     * panel pueda distinguir «recien subida» de «denunciada por tres personas»,
     * que no merecen la misma atencion.</p>
     */
    @Transactional(readOnly = true)
    public List<FotoModeracionResponse> fotosPendientes() {
        return fotoRepository
                .pendientesConDetalle(
                        List.of(EstadoFoto.PENDIENTE, EstadoFoto.EN_REVISION),
                        PageRequest.of(0, TAMANO_BANDEJA))
                .stream()
                .map(foto -> new FotoModeracionResponse(
                        foto.getId(),
                        foto.getCloudinaryUrl(),
                        foto.getUsuario().getNombre(),
                        foto.getLugar().getSlug(),
                        foto.getEstado(),
                        foto.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void aprobarFoto(UUID fotoId) {
        Foto foto = buscarFoto(fotoId);
        EstadoFoto anterior = foto.getEstado();

        foto.setEstado(EstadoFoto.APROBADA);
        foto.setMotivoRechazo(null);
        fotoRepository.save(foto);

        auditoria.registrar("APROBAR_FOTO", "Foto", fotoId, Map.of(
                "lugar", foto.getLugar().getSlug(),
                "estadoAnterior", anterior.name()));

        // La ficha del lugar esta cacheada con ISR: sin este aviso, la foto
        // recien aprobada no aparecetria hasta que caducara el cache.
        eventos.publishEvent(new LugarGuardadoEvent(foto.getLugar().getSlug()));
    }

    /**
     * Rechaza una foto y borra el binario del CDN.
     *
     * <p>La fila <strong>se conserva</strong> con estado {@code RECHAZADA} y su
     * motivo: deja trazabilidad de la decision y permite detectar a quien sube
     * repetidamente contenido inadecuado. Lo que desaparece es el archivo, que
     * es lo que ocupa almacenamiento del plan.</p>
     *
     * <p>Si Cloudinary no responde, la foto queda rechazada igualmente: manda
     * el estado en nuestra base, y un binario huerfano es un problema de coste,
     * no de seguridad. Queda anotado en el log para poder limpiarlo.</p>
     */
    @Transactional
    public void rechazarFoto(UUID fotoId, String motivo) {
        Foto foto = buscarFoto(fotoId);

        boolean borrada = cloudinary.borrar(foto.getCloudinaryPublicId());
        if (!borrada) {
            log.warn("Foto {} rechazada pero su binario sigue en Cloudinary ({})",
                    fotoId, foto.getCloudinaryPublicId());
        }

        EstadoFoto anterior = foto.getEstado();
        foto.setEstado(EstadoFoto.RECHAZADA);
        foto.setMotivoRechazo(motivo != null && !motivo.isBlank() ? motivo.trim() : null);
        fotoRepository.save(foto);

        auditoria.registrar("RECHAZAR_FOTO", "Foto", fotoId, Map.of(
                "lugar", foto.getLugar().getSlug(),
                "estadoAnterior", anterior.name(),
                "motivo", foto.getMotivoRechazo() == null ? "" : foto.getMotivoRechazo(),
                "binarioBorrado", String.valueOf(borrada)));
    }

    // ---------------------------------------------------------------
    //  Reseñas (RF-50)
    // ---------------------------------------------------------------

    /**
     * Reseñas revisables: las publicadas y las ya ocultas.
     *
     * <p>Se incluyen las ocultas a proposito, para poder deshacer: una
     * moderacion sin marcha atras acaba siendo una moderacion que nadie se
     * atreve a usar.</p>
     */
    @Transactional(readOnly = true)
    public List<ResenaModeracionResponse> resenasParaModerar() {
        return resenaRepository
                .paraModerar(List.of(EstadoResena.PUBLICADA, EstadoResena.OCULTA,
                        EstadoResena.EN_REVISION), PageRequest.of(0, TAMANO_BANDEJA))
                .stream()
                .map(this::aRespuestaModeracion)
                .toList();
    }

    /**
     * Oculta una reseña.
     *
     * <p>Deja de contar en el promedio, porque la vista materializada solo suma
     * las {@code PUBLICADA}. El evento fuerza el recalculo inmediato: ocultar
     * una reseña de una estrella debe mover el promedio con la misma rapidez
     * con que lo movio al publicarse.</p>
     */
    @Transactional
    public void ocultarResena(UUID resenaId) {
        cambiarEstado(resenaId, EstadoResena.OCULTA);
    }

    /** Devuelve una reseña oculta a la vista publica. */
    @Transactional
    public void restaurarResena(UUID resenaId) {
        cambiarEstado(resenaId, EstadoResena.PUBLICADA);
    }

    private void cambiarEstado(UUID resenaId, EstadoResena estado) {
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("resena", resenaId.toString()));

        EstadoResena anterior = resena.getEstado();
        resena.setEstado(estado);
        resenaRepository.save(resena);

        auditoria.registrar(
                estado == EstadoResena.OCULTA ? "OCULTAR_RESENA" : "RESTAURAR_RESENA",
                "Resena", resenaId, Map.of(
                        "lugar", resena.getLugar().getSlug(),
                        "estadoAnterior", anterior.name()));

        eventos.publishEvent(new ContenidoCalificadoEvent(
                resena.getLugar().getId(), resena.getLugar().getSlug()));
    }

    private ResenaModeracionResponse aRespuestaModeracion(Resena resena) {
        boolean editada = resena.getUpdatedAt() != null
                && resena.getCreatedAt() != null
                && resena.getUpdatedAt().isAfter(resena.getCreatedAt().plusSeconds(1));

        return new ResenaModeracionResponse(
                resena.getId(),
                resena.getCalificacion(),
                resena.getComentario(),
                resena.getUsuario().getNombre(),
                resena.getLugar().getSlug(),
                resena.getEstado(),
                resena.getCreatedAt(),
                editada);
    }

    private Foto buscarFoto(UUID fotoId) {
        return fotoRepository.findById(fotoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("foto", fotoId.toString()));
    }
}
