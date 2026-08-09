package com.huamanga.tourism.negocio.service;

import com.huamanga.tourism.admin.service.RegistroActividadService;
import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.negocio.domain.EstadoNegocio;
import com.huamanga.tourism.negocio.domain.Negocio;
import com.huamanga.tourism.negocio.dto.NegocioResponse;
import com.huamanga.tourism.negocio.mapper.NegocioMapper;
import com.huamanga.tourism.negocio.repository.NegocioRepository;
import com.huamanga.tourism.usuario.domain.NombreRol;
import com.huamanga.tourism.usuario.repository.RolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aprobacion de negocios por el administrador (RF-104).
 *
 * <p>Es la puerta que separa «alguien pidio aparecer» de «aparece». Sin ella el
 * directorio de una aplicacion municipal se llena en una semana de negocios
 * inventados, duplicados y de propaganda.</p>
 */
@Service
public class AdminNegocioService {

    private final NegocioRepository negocioRepository;
    private final RolRepository rolRepository;
    private final NegocioMapper mapper;
    private final RegistroActividadService auditoria;

    public AdminNegocioService(NegocioRepository negocioRepository,
                               RolRepository rolRepository,
                               NegocioMapper mapper,
                               RegistroActividadService auditoria) {
        this.negocioRepository = negocioRepository;
        this.rolRepository = rolRepository;
        this.mapper = mapper;
        this.auditoria = auditoria;
    }

    /** Bandeja de moderacion: todos, con los pendientes arriba. */
    @Transactional(readOnly = true)
    public List<NegocioResponse> bandeja(Idioma idioma) {
        return negocioRepository.findTodosParaModerar().stream()
                .map(negocio -> mapper.aRespuesta(negocio, idioma))
                .toList();
    }

    /**
     * Cambia el estado de un negocio y deja constancia.
     *
     * <p><strong>Al aprobar se concede el rol NEGOCIO a su dueno</strong>, no
     * al registrarse. Pedir algo no puede otorgar el permiso de tenerlo; es la
     * aprobacion la que convierte a alguien en dueno verificado.</p>
     *
     * <p>Al retirar la aprobacion <strong>no se le quita el rol</strong>: puede
     * gestionar otros negocios, y ademas el rol no le da acceso a nada que no
     * sea suyo —de eso se encarga {@link GuardaDePropiedad}—, asi que quitarlo
     * no protegeria nada y si le cerraria su propio panel.</p>
     */
    @Transactional
    public NegocioResponse cambiarEstado(UUID negocioId, EstadoNegocio nuevo,
                                         String motivo, Idioma idioma) {
        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("negocio", negocioId.toString()));

        EstadoNegocio anterior = negocio.getEstado();
        negocio.setEstado(nuevo);

        if (nuevo == EstadoNegocio.APROBADO
                && negocio.getUsuario().getRol().getNombre() == NombreRol.USUARIO) {
            rolRepository.findByNombre(NombreRol.NEGOCIO)
                    .ifPresent(rol -> negocio.getUsuario().setRol(rol));
        }

        Negocio guardado = negocioRepository.save(negocio);

        Map<String, String> detalles = new HashMap<>();
        detalles.put("negocio", negocio.getNombre());
        detalles.put("estadoAnterior", anterior.name());
        detalles.put("estadoNuevo", nuevo.name());
        if (motivo != null && !motivo.isBlank()) {
            // Es lo que despues lee el dueno en su panel: el motivo vive en la
            // bitacora, sin necesidad de una columna nueva en `negocio`.
            detalles.put("motivo", motivo.trim());
        }

        auditoria.registrar(accionDe(nuevo), "Negocio", negocioId, detalles);

        return mapper.aRespuesta(guardado, idioma);
    }

    private String accionDe(EstadoNegocio estado) {
        return switch (estado) {
            case APROBADO -> "APROBAR_NEGOCIO";
            case RECHAZADO -> "RECHAZAR_NEGOCIO";
            case SUSPENDIDO -> "SUSPENDER_NEGOCIO";
            case PENDIENTE -> "DEVOLVER_NEGOCIO_A_REVISION";
        };
    }
}
