package com.huamanga.tourism.insignia.service;

import com.huamanga.tourism.checkin.domain.CheckIn;
import com.huamanga.tourism.checkin.repository.CheckInRepository;
import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.insignia.domain.Insignia;
import com.huamanga.tourism.insignia.domain.InsigniaUsuario;
import com.huamanga.tourism.insignia.dto.PasaporteResponse;
import com.huamanga.tourism.insignia.repository.InsigniaRepository;
import com.huamanga.tourism.insignia.repository.InsigniaUsuarioRepository;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import com.huamanga.tourism.ruta.domain.RutaTematica;
import com.huamanga.tourism.ruta.repository.RutaTematicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pasaporte patrimonial (RF-39b).
 *
 * <p>Todo lo que devuelve se calcula al vuelo. No hay ninguna tabla de
 * progreso, ni contador de sellos, ni porcentaje guardado: son atributos
 * derivados de los check-ins, y almacenarlos exigiria mantenerlos
 * sincronizados con triggers. Es el mismo criterio que con la calificacion
 * promedio del Bloque 6.</p>
 */
@Service
public class PasaporteService {

    private final CheckInRepository checkInRepository;
    private final InsigniaRepository insigniaRepository;
    private final InsigniaUsuarioRepository insigniaUsuarioRepository;
    private final RutaTematicaRepository rutaRepository;
    private final LugarRepository lugarRepository;

    public PasaporteService(CheckInRepository checkInRepository,
                            InsigniaRepository insigniaRepository,
                            InsigniaUsuarioRepository insigniaUsuarioRepository,
                            RutaTematicaRepository rutaRepository,
                            LugarRepository lugarRepository) {
        this.checkInRepository = checkInRepository;
        this.insigniaRepository = insigniaRepository;
        this.insigniaUsuarioRepository = insigniaUsuarioRepository;
        this.rutaRepository = rutaRepository;
        this.lugarRepository = lugarRepository;
    }

    @Transactional(readOnly = true)
    public PasaporteResponse mio(Idioma idioma) {
        UUID usuarioId = UsuarioActual.idObligatorio();

        return new PasaporteResponse(
                checkInRepository.contarLugaresDistintos(usuarioId),
                // La entidad Lugar lleva @SQLRestriction("deleted_at IS NULL"),
                // asi que este contador ya excluye las bajas logicas.
                lugarRepository.countByEstado(EstadoLugar.PUBLICADO),
                sellos(usuarioId, idioma),
                insignias(usuarioId, idioma),
                progresoDeRutas(usuarioId, idioma));
    }

    /**
     * Un sello por lugar, con la fecha de la PRIMERA visita.
     *
     * <p>El historial viene ordenado de mas reciente a mas antiguo, asi que se
     * recorre entero y se deja siempre el ultimo visto de cada lugar: ese es el
     * mas antiguo. Interesa esa fecha y no la ultima porque el sello conmemora
     * cuando se descubrio el sitio.</p>
     */
    private List<PasaporteResponse.Sello> sellos(UUID usuarioId, Idioma idioma) {
        Map<UUID, PasaporteResponse.Sello> porLugar = new LinkedHashMap<>();

        for (CheckIn visita : checkInRepository.historialDe(usuarioId)) {
            Lugar lugar = visita.getLugar();
            porLugar.put(lugar.getId(), new PasaporteResponse.Sello(
                    lugar.getId(),
                    lugar.getSlug(),
                    nombreDe(lugar, idioma),
                    lugar.getCategoria().getCodigo(),
                    lugar.getCategoria().getColorHex(),
                    visita.getCreatedAt()));
        }

        return porLugar.values().stream()
                .sorted(Comparator.comparing(PasaporteResponse.Sello::visitadoEn).reversed())
                .toList();
    }

    private List<PasaporteResponse.InsigniaResponse> insignias(UUID usuarioId, Idioma idioma) {
        Map<UUID, Instant> obtenidas = insigniaUsuarioRepository
                .findByUsuarioConInsignia(usuarioId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        iu -> iu.getInsignia().getId(), InsigniaUsuario::getObtenidaEn));

        return insigniaRepository.findAllConTraducciones().stream()
                .sorted(Comparator.comparing(Insignia::getOrden))
                .map(insignia -> {
                    var traduccion = insignia.getTraducciones().stream()
                            .filter(t -> t.getId().getIdioma() == idioma)
                            .findFirst()
                            .or(() -> insignia.getTraducciones().stream()
                                    .filter(t -> t.getId().getIdioma() == Idioma.ES)
                                    .findFirst());

                    return new PasaporteResponse.InsigniaResponse(
                            insignia.getId(),
                            insignia.getCodigo(),
                            traduccion.map(t -> t.getNombre()).orElse(insignia.getCodigo()),
                            traduccion.map(t -> t.getDescripcion()).orElse(null),
                            insignia.getIcono(),
                            obtenidas.containsKey(insignia.getId()),
                            obtenidas.get(insignia.getId()));
                })
                .toList();
    }

    private List<PasaporteResponse.ProgresoRuta> progresoDeRutas(UUID usuarioId, Idioma idioma) {
        return rutaRepository.findActivasConRecorrido().stream()
                .map(ruta -> {
                    // Solo cuentan las paradas publicadas: si un lugar se
                    // despublica, exigir su visita haria la ruta imposible de
                    // completar y el progreso se quedaria atascado para siempre.
                    long total = ruta.getLugares().stream()
                            .filter(enlace -> enlace.getLugar() != null
                                    && enlace.getLugar().getEstado() == EstadoLugar.PUBLICADO)
                            .count();

                    long visitados = checkInRepository
                            .contarLugaresVisitadosDeRuta(usuarioId, ruta.getId());

                    return new PasaporteResponse.ProgresoRuta(
                            ruta.getId(),
                            ruta.getSlug(),
                            nombreDe(ruta, idioma),
                            ruta.getColorHex(),
                            visitados,
                            total,
                            total > 0 && visitados >= total);
                })
                .toList();
    }

    private String nombreDe(Lugar lugar, Idioma idioma) {
        return lugar.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> lugar.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst())
                .map(t -> t.getNombre())
                .orElse(lugar.getSlug());
    }

    private String nombreDe(RutaTematica ruta, Idioma idioma) {
        return ruta.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> ruta.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst())
                .map(t -> t.getNombre())
                .orElse(ruta.getSlug());
    }
}
