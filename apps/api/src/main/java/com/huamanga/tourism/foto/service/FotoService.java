package com.huamanga.tourism.foto.service;

import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.foto.domain.EstadoFoto;
import com.huamanga.tourism.foto.domain.Foto;
import com.huamanga.tourism.foto.dto.FotoResponse;
import com.huamanga.tourism.foto.repository.FotoRepository;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.evento.LugarGuardadoEvent;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import com.huamanga.tourism.usuario.domain.Usuario;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Subida y consulta de fotos de lugares (RF-38).
 *
 * <p>Toda foto nace {@code PENDIENTE}. La galeria publica solo muestra las
 * {@code APROBADA}, de modo que nada llega a verse sin pasar por moderacion:
 * es contenido subido por desconocidos sobre patrimonio real.</p>
 */
@Service
public class FotoService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(FotoService.class);

    /** RF-38: hasta 5 fotos por persona y lugar. */
    public static final int MAXIMO_POR_USUARIO_Y_LUGAR = 5;

    private final FotoRepository fotoRepository;
    private final LugarRepository lugarRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidadorImagen validador;
    private final ClienteCloudinary cloudinary;
    private final TransformacionesCloudinary transformaciones;
    private final ApplicationEventPublisher eventos;

    public FotoService(FotoRepository fotoRepository,
                       LugarRepository lugarRepository,
                       UsuarioRepository usuarioRepository,
                       ValidadorImagen validador,
                       ClienteCloudinary cloudinary,
                       TransformacionesCloudinary transformaciones,
                       ApplicationEventPublisher eventos) {
        this.fotoRepository = fotoRepository;
        this.lugarRepository = lugarRepository;
        this.usuarioRepository = usuarioRepository;
        this.validador = validador;
        this.cloudinary = cloudinary;
        this.transformaciones = transformaciones;
        this.eventos = eventos;
    }

    // ---------------------------------------------------------------
    //  Lectura publica
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<FotoResponse> aprobadasDe(String slugLugar) {
        Lugar lugar = lugarPublicado(slugLugar);
        return fotoRepository.findByLugarIdAndEstadoConAutor(lugar.getId(), EstadoFoto.APROBADA)
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    /** Las fotos del usuario actual en un lugar, incluidas las pendientes. */
    @Transactional(readOnly = true)
    public List<FotoResponse> miasEn(String slugLugar) {
        Lugar lugar = lugarPublicado(slugLugar);
        return fotoRepository
                .findByUsuarioIdAndLugarIdConAutor(UsuarioActual.idObligatorio(), lugar.getId())
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    // ---------------------------------------------------------------
    //  Subida
    // ---------------------------------------------------------------

    /**
     * Valida, sube a Cloudinary y registra la foto.
     *
     * <p>El orden importa: <strong>primero se valida, despues se sube</strong>.
     * Al reves, un archivo malicioso ya estaria alojado en el CDN cuando
     * decidieramos rechazarlo.</p>
     *
     * <p>La subida remota ocurre <em>antes</em> de abrir la escritura en base:
     * si Cloudinary falla, no queda ninguna fila apuntando a una imagen que no
     * existe. El riesgo inverso —imagen subida y fila no guardada— deja un
     * binario huerfano, que es el fallo barato de los dos.</p>
     */
    @Transactional
    public FotoResponse subir(String slugLugar, MultipartFile archivo) {
        Lugar lugar = lugarPublicado(slugLugar);
        UUID usuarioId = UsuarioActual.idObligatorio();

        long yaSubidas = fotoRepository.countByUsuarioIdAndLugarId(usuarioId, lugar.getId());
        if (yaSubidas >= MAXIMO_POR_USUARIO_Y_LUGAR) {
            throw new DemasiadasFotosException(MAXIMO_POR_USUARIO_Y_LUGAR);
        }

        ValidadorImagen.Formato formato = validador.validar(archivo);

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new ValidadorImagen.ImagenInvalidaException("archivo-ilegible");
        }

        // El identificador lo genera el servidor. El nombre que envio el
        // cliente no interviene: asi no hay recorrido de rutas ni colisiones.
        String publicId = "%s/%s/%s".formatted(
                "lugares", lugar.getSlug(), UUID.randomUUID());

        ClienteCloudinary.Resultado subida = cloudinary.subir(contenido, publicId);

        Usuario autor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario", usuarioId.toString()));

        Foto foto = new Foto();
        foto.setUsuario(autor);
        foto.setLugar(lugar);
        foto.setCloudinaryUrl(subida.url());
        foto.setCloudinaryPublicId(subida.publicId());
        foto.setEstado(EstadoFoto.PENDIENTE);

        Foto guardada = fotoRepository.save(foto);
        log.info("Foto {} subida a {} ({}), pendiente de moderacion",
                guardada.getId(), lugar.getSlug(), formato);

        return aRespuesta(guardada);
    }

    // ---------------------------------------------------------------
    //  Interno
    // ---------------------------------------------------------------

    private Lugar lugarPublicado(String slug) {
        return lugarRepository.findBySlug(slug)
                .filter(l -> l.getEstado() == EstadoLugar.PUBLICADO)
                .orElseThrow(() -> new RecursoNoEncontradoException("lugar", slug));
    }

    private FotoResponse aRespuesta(Foto foto) {
        return new FotoResponse(
                foto.getId(),
                transformaciones.paraEntrega(foto.getCloudinaryUrl()),
                transformaciones.paraMiniatura(foto.getCloudinaryUrl()),
                foto.getUsuario().getNombre(),
                foto.getEstado(),
                foto.getMotivoRechazo(),
                foto.getCreatedAt());
    }

    /** Avisa de que la galeria del lugar cambio, para revalidar su pagina. */
    void avisarGaleriaCambiada(Lugar lugar) {
        eventos.publishEvent(new LugarGuardadoEvent(lugar.getSlug()));
    }

    public static class DemasiadasFotosException extends RuntimeException {
        public DemasiadasFotosException(int maximo) {
            super("Solo puedes subir " + maximo + " fotos por lugar");
        }
    }
}
