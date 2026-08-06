package com.huamanga.tourism.reporte.service;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.foto.service.ClienteCloudinary;
import com.huamanga.tourism.foto.service.TransformacionesCloudinary;
import com.huamanga.tourism.foto.service.ValidadorImagen;
import com.huamanga.tourism.reporte.domain.EstadoReporte;
import com.huamanga.tourism.reporte.domain.FotoReporte;
import com.huamanga.tourism.reporte.domain.Reporte;
import com.huamanga.tourism.reporte.domain.TipoIncidente;
import com.huamanga.tourism.reporte.dto.ReporteRequest;
import com.huamanga.tourism.reporte.dto.ReporteResponse;
import com.huamanga.tourism.reporte.repository.ReporteRepository;
import com.huamanga.tourism.reporte.repository.TipoIncidenteRepository;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Reportes ciudadanos de atentados al patrimonio (RF-69 a RF-74).
 *
 * <p>El diferenciador del proyecto: invierte la direccion del dato. En lugar de
 * que la institucion informe al ciudadano, es el ciudadano quien avisa de que
 * algo esta danando el patrimonio.</p>
 *
 * <p><strong>Sobre el anonimato.</strong> Este service colabora, pero no es
 * quien lo garantiza: la limpieza definitiva la hace {@code Reporte} en su
 * {@code @PrePersist}, de modo que la promesa no dependa de que este codigo
 * —ni ningun otro futuro— se acuerde de cumplirla.</p>
 */
@Service
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    /** RF-73: hasta 5 fotos por reporte. */
    public static final int MAXIMO_FOTOS = 5;

    private static final GeometryFactory GEOMETRIAS =
            new GeometryFactory(new PrecisionModel(), 4326);

    // Los mismos limites que valida la base con ck_reporte_bounds_ayacucho.
    private static final double LON_MINIMA = -75.5;
    private static final double LON_MAXIMA = -73.0;
    private static final double LAT_MINIMA = -15.5;
    private static final double LAT_MAXIMA = -12.5;

    private final ReporteRepository reporteRepository;
    private final TipoIncidenteRepository tipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidadorImagen validador;
    private final ClienteCloudinary cloudinary;
    private final TransformacionesCloudinary transformaciones;

    public ReporteService(ReporteRepository reporteRepository,
                          TipoIncidenteRepository tipoRepository,
                          UsuarioRepository usuarioRepository,
                          ValidadorImagen validador,
                          ClienteCloudinary cloudinary,
                          TransformacionesCloudinary transformaciones) {
        this.reporteRepository = reporteRepository;
        this.tipoRepository = tipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.validador = validador;
        this.cloudinary = cloudinary;
        this.transformaciones = transformaciones;
    }

    // ---------------------------------------------------------------
    //  Crear (RF-69 a RF-73)
    // ---------------------------------------------------------------

    /**
     * Registra una denuncia, con o sin identidad.
     *
     * <p>Las fotos se suben <strong>re-codificadas</strong>: se descarta el
     * bloque EXIF, que en una foto de movil contiene la posicion GPS exacta y
     * el modelo del aparato. En una denuncia anonima esa es la fuga mas grave y
     * la menos visible, porque la imagen se ve igual.</p>
     */
    @Transactional
    public ReporteResponse crear(ReporteRequest peticion, List<MultipartFile> fotos, Idioma idioma) {
        validarDentroDeAyacucho(peticion.longitud(), peticion.latitud());

        TipoIncidente tipo = tipoRepository.findById(peticion.tipoIncidenteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "tipo-incidente", peticion.tipoIncidenteId().toString()));

        if (fotos != null && fotos.size() > MAXIMO_FOTOS) {
            throw new DemasiadasFotosException(MAXIMO_FOTOS);
        }

        Reporte reporte = new Reporte();
        reporte.setTipoIncidente(tipo);
        reporte.setDescripcion(peticion.descripcion().trim());
        reporte.setUbicacion(punto(peticion.longitud(), peticion.latitud()));
        reporte.setDireccionReferencial(
                peticion.direccionReferencial() != null && !peticion.direccionReferencial().isBlank()
                        ? peticion.direccionReferencial().trim() : null);
        reporte.setEstado(EstadoReporte.RECIBIDO);
        reporte.setEsAnonimo(peticion.anonimo());

        // Solo si NO es anonimo se adjunta la identidad. Y aun asi, el
        // @PrePersist de la entidad volveria a limpiarla si el indicador
        // dijera lo contrario: aqui no esta la garantia, esta la intencion.
        if (!peticion.anonimo()) {
            UsuarioActual.id().ifPresent(id ->
                    reporte.setUsuario(usuarioRepository.getReferenceById(id)));
            reporte.setNombreReportante(
                    peticion.nombreReportante() != null && !peticion.nombreReportante().isBlank()
                            ? peticion.nombreReportante().trim() : null);
        }

        Reporte guardado = reporteRepository.save(reporte);
        adjuntarFotos(guardado, fotos);

        // El log NO menciona al usuario aunque haya sesion: un rastro en el
        // registro anularia el trabajo de las columnas.
        log.info("Reporte {} recibido ({}), anonimo={}",
                guardado.getId(), tipo.getCodigo(), guardado.isEsAnonimo());

        return aRespuesta(guardado, idioma, false);
    }

    private void adjuntarFotos(Reporte reporte, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty() || !cloudinary.configurado()) {
            return;
        }

        short orden = 0;
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }

            ValidadorImagen.Formato formato = validador.validar(archivo);

            byte[] original;
            try {
                original = archivo.getBytes();
            } catch (IOException e) {
                throw new ValidadorImagen.ImagenInvalidaException("archivo-ilegible");
            }

            // Aqui desaparece el EXIF, antes de que la imagen salga del servidor.
            byte[] limpia = validador.sinMetadatos(original, formato);

            var subida = cloudinary.subir(limpia, "reportes/%s/%s"
                    .formatted(reporte.getId(), UUID.randomUUID()));

            FotoReporte foto = new FotoReporte();
            foto.setReporte(reporte);
            foto.setCloudinaryUrl(subida.url());
            foto.setCloudinaryPublicId(subida.publicId());
            foto.setOrden(orden++);
            reporte.getFotos().add(foto);
        }
    }

    // ---------------------------------------------------------------
    //  Lectura publica (RF-74)
    // ---------------------------------------------------------------

    /**
     * Incidentes visibles en un area del mapa.
     *
     * <p>Solo se publican los <strong>aprobados</strong> y los ya resueltos.
     * Nada llega al mapa sin que una persona lo haya revisado: es contenido que
     * acusa a terceros de danar el patrimonio, y publicar una denuncia falsa o
     * difamatoria sin filtro seria el peor resultado posible de este modulo.</p>
     */
    @Transactional(readOnly = true)
    public List<ReporteResponse> enMapa(double oeste, double sur, double este, double norte,
                                        Idioma idioma) {
        return reporteRepository
                .buscarPublicadosEnArea(oeste, sur, este, norte)
                .stream()
                .map(reporte -> aRespuesta(reporte, idioma, false))
                .toList();
    }

    /** Reportes que envio el usuario actual, sin incluir los anonimos. */
    @Transactional(readOnly = true)
    public List<ReporteResponse> mios(Idioma idioma) {
        return reporteRepository.findByUsuarioIdOrderByCreatedAtDesc(UsuarioActual.idObligatorio())
                .stream()
                .map(reporte -> aRespuesta(reporte, idioma, false))
                .toList();
    }

    // ---------------------------------------------------------------
    //  Interno
    // ---------------------------------------------------------------

    /**
     * Rechaza coordenadas fuera de la region (RF-22b).
     *
     * <p>La base tiene el mismo CHECK, pero devolveria un error de integridad
     * ilegible. Aqui se convierte en un 400 que dice que pasa.</p>
     */
    private void validarDentroDeAyacucho(double longitud, double latitud) {
        boolean dentro = longitud >= LON_MINIMA && longitud <= LON_MAXIMA
                && latitud >= LAT_MINIMA && latitud <= LAT_MAXIMA;

        if (!dentro) {
            throw new FueraDeAyacuchoException();
        }
    }

    ReporteResponse aRespuesta(Reporte reporte, Idioma idioma, boolean paraModeracion) {
        TipoIncidente tipo = reporte.getTipoIncidente();

        String nombreTipo = tipo.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> tipo.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst())
                .map(t -> t.getNombre())
                .orElse(tipo.getCodigo());

        List<String> urls = new ArrayList<>(reporte.getFotos().stream()
                .sorted(Comparator.comparing(FotoReporte::getOrden))
                .map(foto -> transformaciones.paraEntrega(foto.getCloudinaryUrl()))
                .toList());

        return new ReporteResponse(
                reporte.getId(),
                tipo.getCodigo(),
                nombreTipo,
                tipo.getIcono(),
                tipo.getColorHex(),
                reporte.getDescripcion(),
                reporte.getUbicacion().getX(),
                reporte.getUbicacion().getY(),
                reporte.getDireccionReferencial(),
                reporte.getEstado(),
                reporte.isEsAnonimo(),
                urls,
                // Las notas internas solo salen en la bandeja de moderacion.
                paraModeracion ? reporte.getNotasAdmin() : null,
                reporte.getCreatedAt());
    }

    private Point punto(double longitud, double latitud) {
        Point punto = GEOMETRIAS.createPoint(new Coordinate(longitud, latitud));
        punto.setSRID(4326);
        return punto;
    }

    public static class FueraDeAyacuchoException extends RuntimeException {
        public FueraDeAyacuchoException() {
            super("Las coordenadas quedan fuera de la region Ayacucho");
        }
    }

    public static class DemasiadasFotosException extends RuntimeException {
        public DemasiadasFotosException(int maximo) {
            super("Un reporte admite hasta " + maximo + " fotos");
        }
    }
}
