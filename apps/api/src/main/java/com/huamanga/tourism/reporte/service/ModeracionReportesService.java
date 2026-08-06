package com.huamanga.tourism.reporte.service;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.insignia.service.MotorInsignias;
import com.huamanga.tourism.reporte.domain.EstadoReporte;
import com.huamanga.tourism.reporte.domain.Reporte;
import com.huamanga.tourism.reporte.dto.ReporteResponse;
import com.huamanga.tourism.reporte.repository.ReporteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Bandeja de moderacion de reportes ciudadanos (RF-76).
 *
 * <p>Es el filtro que separa una denuncia util de una difamacion. Por eso nada
 * llega al mapa publico sin pasar por aqui.</p>
 */
@Service
public class ModeracionReportesService {

    private static final Logger log = LoggerFactory.getLogger(ModeracionReportesService.class);

    private static final int TAMANO_BANDEJA = 100;

    private final ReporteRepository reporteRepository;
    private final ReporteService reporteService;
    private final MotorInsignias motorInsignias;

    public ModeracionReportesService(ReporteRepository reporteRepository,
                                     ReporteService reporteService,
                                     MotorInsignias motorInsignias) {
        this.reporteRepository = reporteRepository;
        this.reporteService = reporteService;
        this.motorInsignias = motorInsignias;
    }

    @Transactional(readOnly = true)
    public List<ReporteResponse> bandeja(Idioma idioma) {
        return reporteRepository.paraModerar(PageRequest.of(0, TAMANO_BANDEJA)).stream()
                // true: aqui SI viajan las notas internas.
                .map(reporte -> reporteService.aRespuesta(reporte, idioma, true))
                .toList();
    }

    /**
     * Cambia el estado y, si procede, concede la insignia GUARDIAN.
     *
     * <p><strong>La insignia solo puede darse en reportes no anonimos</strong>,
     * y no por una limitacion tecnica sino por coherencia: un reporte anonimo
     * no tiene a quien atribuirse. No se puede ser anonimo y recibir credito a
     * la vez. El formulario lo advierte antes de enviar, para que la persona
     * elija sabiendo el precio.</p>
     *
     * <p>La evaluacion va en la misma transaccion que el cambio de estado, por
     * el mismo motivo que en el check-in del Bloque 7: si se hiciera despues,
     * una caida en medio dejaria el reporte aprobado y la insignia perdida.</p>
     */
    @Transactional
    public void cambiarEstado(UUID reporteId, EstadoReporte nuevoEstado, String notas) {
        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("reporte", reporteId.toString()));

        EstadoReporte anterior = reporte.getEstado();
        reporte.setEstado(nuevoEstado);

        if (notas != null && !notas.isBlank()) {
            reporte.setNotasAdmin(notas.trim());
        }

        reporteRepository.save(reporte);

        // `updated_by` si se rellena aqui, y debe hacerlo: identifica al
        // administrador que tomo la decision, no al denunciante. Esa
        // trazabilidad es justo la que interesa conservar.
        log.info("Reporte {} pasa de {} a {}", reporteId, anterior, nuevoEstado);

        boolean seAprueba = nuevoEstado == EstadoReporte.APROBADO
                || nuevoEstado == EstadoReporte.RESUELTO;

        if (seAprueba && reporte.getUsuario() != null) {
            motorInsignias.evaluar(reporte.getUsuario().getId());
        }
    }
}
