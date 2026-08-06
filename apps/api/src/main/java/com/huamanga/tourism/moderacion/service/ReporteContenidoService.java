package com.huamanga.tourism.moderacion.service;

import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.foto.domain.EstadoFoto;
import com.huamanga.tourism.foto.domain.Foto;
import com.huamanga.tourism.foto.repository.FotoRepository;
import com.huamanga.tourism.moderacion.domain.ReporteContenido;
import com.huamanga.tourism.moderacion.dto.ReporteContenidoRequest;
import com.huamanga.tourism.moderacion.repository.ReporteContenidoRepository;
import com.huamanga.tourism.resena.domain.EstadoResena;
import com.huamanga.tourism.resena.domain.Resena;
import com.huamanga.tourism.resena.repository.ResenaRepository;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reportes de contenido inapropiado (RF-45).
 *
 * <p><strong>Por que dos FK nullables y no una polimorfica.</strong> Una sola
 * columna {@code contenido_id} con un {@code tipo} al lado seria mas compacta,
 * pero PostgreSQL no puede validar una FK que apunte a dos tablas: se perderia
 * la integridad referencial y el {@code ON DELETE CASCADE}. Con dos columnas,
 * cada una tiene su {@code REFERENCES} de verdad y un CHECK garantiza que solo
 * una este rellena.</p>
 */
@Service
public class ReporteContenidoService {

    private static final Logger log = LoggerFactory.getLogger(ReporteContenidoService.class);

    /** Al tercer reporte de personas distintas, el contenido pasa a revision. */
    public static final int REPORTES_PARA_REVISION = 3;

    private final ReporteContenidoRepository reporteRepository;
    private final FotoRepository fotoRepository;
    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;

    public ReporteContenidoService(ReporteContenidoRepository reporteRepository,
                                   FotoRepository fotoRepository,
                                   ResenaRepository resenaRepository,
                                   UsuarioRepository usuarioRepository) {
        this.reporteRepository = reporteRepository;
        this.fotoRepository = fotoRepository;
        this.resenaRepository = resenaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra un reporte y, si alcanza el umbral, manda el contenido a revision.
     *
     * @return true si este reporte fue el que activo la revision
     */
    @Transactional
    public boolean reportar(ReporteContenidoRequest peticion) {
        UUID usuarioId = UsuarioActual.idObligatorio();

        ReporteContenido reporte = new ReporteContenido();
        reporte.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        reporte.setMotivo(peticion.motivo());

        boolean activaRevision;

        if (peticion.fotoId() != null) {
            Foto foto = fotoRepository.findById(peticion.fotoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "foto", peticion.fotoId().toString()));

            if (reporteRepository.existsByUsuarioIdAndFotoId(usuarioId, foto.getId())) {
                throw new ReporteDuplicadoException();
            }

            reporte.setFoto(foto);
            reporteRepository.save(reporte);

            // El indice unico parcial (usuario_id, foto_id) garantiza que cada
            // fila sea de una persona distinta, asi que contar filas equivale a
            // contar denunciantes: no hace falta un COUNT DISTINCT.
            activaRevision = reporteRepository.countByFotoId(foto.getId()) >= REPORTES_PARA_REVISION
                    && foto.getEstado() != EstadoFoto.EN_REVISION;

            if (activaRevision) {
                foto.setEstado(EstadoFoto.EN_REVISION);
                fotoRepository.save(foto);
                log.info("Foto {} a revision por {} reportes", foto.getId(), REPORTES_PARA_REVISION);
            }

        } else {
            Resena resena = resenaRepository.findById(peticion.resenaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "resena", peticion.resenaId().toString()));

            // Reportar la propia reseña no tiene sentido y ensuciaria el conteo.
            if (resena.getUsuario().getId().equals(usuarioId)) {
                throw new AutorreporteException();
            }

            if (reporteRepository.existsByUsuarioIdAndResenaId(usuarioId, resena.getId())) {
                throw new ReporteDuplicadoException();
            }

            reporte.setResena(resena);
            reporteRepository.save(reporte);

            activaRevision = reporteRepository.countByResenaId(resena.getId()) >= REPORTES_PARA_REVISION
                    && resena.getEstado() != EstadoResena.EN_REVISION;

            if (activaRevision) {
                // EN_REVISION la saca de la lista publica —que solo muestra
                // PUBLICADA— y del promedio, que la vista materializada calcula
                // igualmente solo con las publicadas. Se retira sola mientras
                // un administrador decide, sin borrar nada.
                resena.setEstado(EstadoResena.EN_REVISION);
                resenaRepository.save(resena);
                log.info("Resena {} a revision por {} reportes", resena.getId(), REPORTES_PARA_REVISION);
            }
        }

        return activaRevision;
    }

    /** Una misma persona no puede reportar dos veces el mismo contenido. */
    public static class ReporteDuplicadoException extends RuntimeException {
        public ReporteDuplicadoException() {
            super("Ya reportaste este contenido");
        }
    }

    public static class AutorreporteException extends RuntimeException {
        public AutorreporteException() {
            super("No tiene sentido reportar tu propio contenido");
        }
    }
}
