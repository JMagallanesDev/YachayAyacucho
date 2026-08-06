package com.huamanga.tourism.insignia.service;

import com.huamanga.tourism.checkin.repository.CheckInRepository;
import com.huamanga.tourism.foto.domain.EstadoFoto;
import com.huamanga.tourism.foto.repository.FotoRepository;
import com.huamanga.tourism.insignia.domain.Insignia;
import com.huamanga.tourism.insignia.domain.InsigniaUsuario;
import com.huamanga.tourism.insignia.repository.InsigniaRepository;
import com.huamanga.tourism.insignia.repository.InsigniaUsuarioRepository;
import com.huamanga.tourism.resena.domain.EstadoResena;
import com.huamanga.tourism.resena.repository.ResenaRepository;
import com.huamanga.tourism.ruta.repository.RutaTematicaRepository;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Concede insignias evaluando el criterio JSONB (RF-39b).
 *
 * <p><strong>Por que el criterio es JSON y no columnas.</strong> Cada insignia
 * se gana de una forma distinta: unas cuentan visitas, otras visitas de una
 * categoria, otras resenas. Modelarlo con columnas obligaria a una tabla llena
 * de nulos o a una tabla por tipo. Como la base <em>nunca consulta dentro</em>
 * del JSON —lo interpreta este service—, no hay atributo multivaluado y la 1FN
 * queda intacta: es configuracion, no dato relacional.</p>
 *
 * <p><strong>Anadir una insignia es un INSERT.</strong> Mientras el {@code tipo}
 * sea uno de los conocidos, no hace falta desplegar nada.</p>
 *
 * <p><strong>Un tipo desconocido se ignora.</strong> Si alguien siembra un
 * criterio que este codigo no entiende, lo peor que ocurre es que la insignia
 * no se conceda; jamas que falle el check-in que disparo la evaluacion. Perder
 * una visita real por un error de configuracion seria mucho peor que no dar un
 * premio.</p>
 */
@Service
public class MotorInsignias {

    private static final Logger log = LoggerFactory.getLogger(MotorInsignias.class);

    private final InsigniaRepository insigniaRepository;
    private final InsigniaUsuarioRepository insigniaUsuarioRepository;
    private final CheckInRepository checkInRepository;
    private final ResenaRepository resenaRepository;
    private final FotoRepository fotoRepository;
    private final RutaTematicaRepository rutaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    private final ObjectMapper json = JsonMapper.builder().build();

    public MotorInsignias(InsigniaRepository insigniaRepository,
                          InsigniaUsuarioRepository insigniaUsuarioRepository,
                          CheckInRepository checkInRepository,
                          ResenaRepository resenaRepository,
                          FotoRepository fotoRepository,
                          RutaTematicaRepository rutaRepository,
                          UsuarioRepository usuarioRepository,
                          Clock clock) {
        this.insigniaRepository = insigniaRepository;
        this.insigniaUsuarioRepository = insigniaUsuarioRepository;
        this.checkInRepository = checkInRepository;
        this.resenaRepository = resenaRepository;
        this.fotoRepository = fotoRepository;
        this.rutaRepository = rutaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    /**
     * Evalua todas las insignias y concede las que correspondan.
     *
     * <p>Se ejecuta <strong>dentro de la transaccion</strong> de la accion que
     * la dispara (el check-in, la resena, la foto aprobada). Es deliberado: si
     * se hiciera despues, en un evento posterior al commit, una caida de red o
     * del proceso justo en medio dejaria la visita registrada y la insignia
     * perdida, y el usuario no tendria forma de recuperarla salvo volver al
     * lugar. Atadas al mismo commit, o se guardan las dos cosas o ninguna.</p>
     *
     * <p>Es idempotente: la PK compuesta {@code (usuario_id, insignia_id)} hace
     * que reevaluar mil veces no duplique nada.</p>
     *
     * @return las insignias recien obtenidas, para poder celebrarlas
     */
    @Transactional
    public List<Insignia> evaluar(UUID usuarioId) {
        List<Insignia> nuevas = new ArrayList<>();

        for (Insignia insignia : insigniaRepository.findAll()) {
            if (insigniaUsuarioRepository.existsByIdUsuarioIdAndIdInsigniaId(usuarioId, insignia.getId())) {
                continue;
            }
            if (cumple(usuarioId, insignia)) {
                conceder(usuarioId, insignia);
                nuevas.add(insignia);
            }
        }

        return nuevas;
    }

    private boolean cumple(UUID usuarioId, Insignia insignia) {
        JsonNode criterio;
        try {
            criterio = json.readTree(insignia.getCriterio());
        } catch (Exception e) {
            log.warn("Insignia {} con criterio ilegible; se ignora: {}",
                    insignia.getCodigo(), e.getMessage());
            return false;
        }

        String tipo = criterio.path("tipo").asString("");
        int cantidad = criterio.path("cantidad").asInt(1);

        return switch (tipo) {
            case "CHECKINS_TOTAL" ->
                    checkInRepository.contarLugaresDistintos(usuarioId) >= cantidad;

            case "CHECKINS_CATEGORIA" -> {
                String categoria = criterio.path("categoria").asString("");
                yield !categoria.isBlank()
                        && checkInRepository.contarLugaresDistintosDeCategoria(usuarioId, categoria) >= cantidad;
            }

            case "RUTA_COMPLETADA" -> rutasCompletadas(usuarioId) >= cantidad;

            case "RESENAS_PUBLICADAS" ->
                    resenaRepository.countByUsuarioIdAndEstado(usuarioId, EstadoResena.PUBLICADA) >= cantidad;

            case "FOTOS_APROBADAS" ->
                    fotoRepository.countByUsuarioIdAndEstado(usuarioId, EstadoFoto.APROBADA) >= cantidad;

            case "REPORTES_APROBADOS" -> {
                // Se refiere a los reportes ciudadanos de preservacion (tabla
                // `reporte`), que llegan en el Bloque 8, y NO a los reportes de
                // contenido de este bloque: son cosas distintas con nombres
                // parecidos. Hasta entonces esta insignia no es obtenible.
                yield false;
            }

            default -> {
                log.debug("Insignia {} con tipo desconocido '{}'; se ignora",
                        insignia.getCodigo(), tipo);
                yield false;
            }
        };
    }

    /** Una ruta esta completada cuando se han visitado TODAS sus paradas. */
    private long rutasCompletadas(UUID usuarioId) {
        return rutaRepository.findActivasConRecorrido().stream()
                .filter(ruta -> {
                    long paradas = ruta.getLugares().size();
                    if (paradas == 0) {
                        return false;
                    }
                    return checkInRepository
                            .contarLugaresVisitadosDeRuta(usuarioId, ruta.getId()) >= paradas;
                })
                .count();
    }

    private void conceder(UUID usuarioId, Insignia insignia) {
        // Se usa el constructor de la entidad, que ademas compone la PK. El
        // sello de tiempo hay que darlo a mano: esta tabla no lleva listener de
        // auditoria, asi que construirla con setters dejaria `obtenida_en` en
        // NULL contra una columna NOT NULL.
        InsigniaUsuario obtenida = new InsigniaUsuario(
                usuarioRepository.getReferenceById(usuarioId),
                insignia,
                clock.instant());

        insigniaUsuarioRepository.save(obtenida);
        log.info("Insignia {} concedida al usuario {}", insignia.getCodigo(), usuarioId);
    }
}
