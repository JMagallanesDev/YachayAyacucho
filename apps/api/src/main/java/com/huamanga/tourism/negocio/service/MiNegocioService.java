package com.huamanga.tourism.negocio.service;

import com.huamanga.tourism.admin.repository.RegistroActividadRepository;
import com.huamanga.tourism.analitica.repository.VisitaNegocioDiarioRepository;
import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.common.tiempo.TiempoAyacucho;
import com.huamanga.tourism.geografia.repository.DistritoRepository;
import com.huamanga.tourism.negocio.domain.EstadoNegocio;
import com.huamanga.tourism.negocio.domain.Negocio;
import com.huamanga.tourism.negocio.domain.NegocioTraduccion;
import com.huamanga.tourism.negocio.domain.NegocioTraduccionId;
import com.huamanga.tourism.negocio.dto.MiNegocioResponse;
import com.huamanga.tourism.negocio.dto.NegocioRequest;
import com.huamanga.tourism.negocio.mapper.NegocioMapper;
import com.huamanga.tourism.negocio.repository.CategoriaNegocioRepository;
import com.huamanga.tourism.negocio.repository.NegocioRepository;
import com.huamanga.tourism.usuario.repository.UsuarioRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * El negocio visto y gestionado por su propio dueno (RF-104, RF-107).
 *
 * <p><strong>Ni un solo metodo de escritura recibe un negocio sin pasar por
 * {@link GuardaDePropiedad}.</strong> Ese es el contrato de esta clase: el rol
 * NEGOCIO no autoriza nada por si mismo, autoriza ser el gestor de esa fila.</p>
 */
@Service
public class MiNegocioService {

    /** SRID 4326 = WGS84, el sistema de coordenadas del GPS. */
    private static final GeometryFactory GEOMETRIAS =
            new GeometryFactory(new PrecisionModel(), 4326);

    /** Todo lo que no sea un digito o un '+' inicial sobra en un numero. */
    private static final Pattern SIGNOS = Pattern.compile("[^0-9]");

    /** Prefijo de Peru, para los numeros escritos en formato local. */
    private static final String PREFIJO_PERU = "51";

    private static final int DIAS_DE_METRICAS = 30;

    private final NegocioRepository negocioRepository;
    private final CategoriaNegocioRepository categoriaRepository;
    private final DistritoRepository distritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VisitaNegocioDiarioRepository visitasRepository;
    private final RegistroActividadRepository actividadRepository;
    private final GuardaDePropiedad guarda;
    private final NegocioMapper mapper;
    private final Clock clock;

    public MiNegocioService(NegocioRepository negocioRepository,
                            CategoriaNegocioRepository categoriaRepository,
                            DistritoRepository distritoRepository,
                            UsuarioRepository usuarioRepository,
                            VisitaNegocioDiarioRepository visitasRepository,
                            RegistroActividadRepository actividadRepository,
                            GuardaDePropiedad guarda,
                            NegocioMapper mapper,
                            Clock clock) {
        this.negocioRepository = negocioRepository;
        this.categoriaRepository = categoriaRepository;
        this.distritoRepository = distritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.visitasRepository = visitasRepository;
        this.actividadRepository = actividadRepository;
        this.guarda = guarda;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Registra un negocio nuevo (RF-104).
     *
     * <p>Nace <strong>PENDIENTE</strong>, siempre. El estado no se puede enviar
     * en la peticion —el record ni siquiera tiene ese campo— asi que no hay
     * forma de publicarse sin pasar por la revision del administrador.</p>
     */
    @Transactional
    public MiNegocioResponse registrar(NegocioRequest peticion, Idioma idioma) {
        UUID yo = UsuarioActual.idObligatorio();

        Negocio negocio = new Negocio();
        negocio.setUsuario(usuarioRepository.getReferenceById(yo));
        negocio.setEstado(EstadoNegocio.PENDIENTE);
        aplicar(peticion, negocio);

        return aRespuesta(negocioRepository.save(negocio), idioma);
    }

    /** Los negocios de quien pregunta, en cualquier estado. */
    @Transactional(readOnly = true)
    public List<MiNegocioResponse> mios(Idioma idioma) {
        return negocioRepository.findMiosConDetalle(UsuarioActual.idObligatorio()).stream()
                .map(negocio -> aRespuesta(negocio, idioma))
                .toList();
    }

    @Transactional(readOnly = true)
    public MiNegocioResponse uno(UUID negocioId, Idioma idioma) {
        return aRespuesta(guarda.mio(negocioId), idioma);
    }

    /**
     * Edita el negocio propio.
     *
     * <p><strong>Si cambia algo que el publico ve, vuelve a PENDIENTE.</strong>
     * Es el mismo criterio que las resenas editadas del Bloque 6: sin esto se
     * aprueba un negocio y se publica otro, que es la forma evidente de burlar
     * la moderacion. Cambiar el horario o el telefono no la dispara —son datos
     * de contacto que envejecen y obligar a revision por eso solo conseguiria
     * que nadie los actualizara nunca—.</p>
     */
    @Transactional
    public MiNegocioResponse actualizar(UUID negocioId, NegocioRequest peticion, Idioma idioma) {
        Negocio negocio = guarda.mio(negocioId);

        boolean cambioLoPublico = !negocio.getNombre().equals(peticion.nombre())
                || !negocio.getCategoria().getId().equals(peticion.categoriaId())
                || descripcionCambio(negocio, peticion);

        aplicar(peticion, negocio);

        if (cambioLoPublico && negocio.getEstado() == EstadoNegocio.APROBADO) {
            negocio.setEstado(EstadoNegocio.PENDIENTE);
        }

        return aRespuesta(negocioRepository.save(negocio), idioma);
    }

    // ---------------------------------------------------------------
    //  Interno
    // ---------------------------------------------------------------

    private boolean descripcionCambio(Negocio negocio, NegocioRequest peticion) {
        if (peticion.traducciones() == null) {
            return false;
        }
        return peticion.traducciones().stream().anyMatch(nueva -> {
            String anterior = negocio.getTraducciones().stream()
                    .filter(t -> t.getId().getIdioma() == nueva.idioma())
                    .findFirst()
                    .map(NegocioTraduccion::getDescripcion)
                    .orElse(null);
            return !java.util.Objects.equals(anterior, nueva.descripcion());
        });
    }

    private void aplicar(NegocioRequest peticion, Negocio negocio) {
        negocio.setNombre(peticion.nombre().trim());
        negocio.setCategoria(categoriaRepository.findById(peticion.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "categoria", peticion.categoriaId().toString())));
        negocio.setDistrito(distritoRepository.findById(peticion.distritoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "distrito", peticion.distritoId().toString())));

        negocio.setRuc(vacioComoNulo(peticion.ruc()));
        negocio.setTelefono(vacioComoNulo(peticion.telefono()));
        negocio.setWhatsapp(normalizarWhatsapp(peticion.whatsapp()));
        negocio.setDireccion(vacioComoNulo(peticion.direccion()));
        negocio.setHorarioTexto(vacioComoNulo(peticion.horarioTexto()));
        negocio.setUbicacion(punto(peticion.longitud(), peticion.latitud()));

        reemplazarTraducciones(peticion, negocio);
    }

    /**
     * Deja el numero listo para {@code wa.me}: solo digitos y con prefijo.
     *
     * <p>Se normaliza <strong>al guardar y no al pintar</strong>. Si se hiciera
     * al pintar, el mismo numero se limpiaria en cada peticion y cualquier sitio
     * que olvidara hacerlo generaria un enlace roto. Guardado limpio, el enlace
     * es una concatenacion.</p>
     *
     * <p>Un numero peruano escrito como «966 123 456» son 9 digitos sin prefijo;
     * se le antepone el 51. Si ya viene con prefijo o es extranjero, se respeta.</p>
     */
    private String normalizarWhatsapp(String crudo) {
        String limpio = crudo == null ? "" : SIGNOS.matcher(crudo).replaceAll("");
        if (limpio.isEmpty()) {
            return null;
        }
        return limpio.length() == 9 ? PREFIJO_PERU + limpio : limpio;
    }

    private String vacioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Point punto(Double longitud, Double latitud) {
        // O van las dos coordenadas o no va ninguna: media coordenada no situa
        // nada y el CHECK de la tabla la rechazaria con un error ilegible.
        if (longitud == null || latitud == null) {
            return null;
        }
        Point punto = GEOMETRIAS.createPoint(new Coordinate(longitud, latitud));
        punto.setSRID(4326);
        return punto;
    }

    /** Vacia y rellena la MISMA coleccion (la trampa de {@code orphanRemoval}). */
    private void reemplazarTraducciones(NegocioRequest peticion, Negocio negocio) {
        negocio.getTraducciones().clear();
        if (peticion.traducciones() == null) {
            return;
        }
        for (var fuente : peticion.traducciones()) {
            if (fuente.descripcion() == null || fuente.descripcion().isBlank()) {
                continue;
            }
            NegocioTraduccion traduccion = new NegocioTraduccion();
            traduccion.setId(new NegocioTraduccionId(negocio.getId(), fuente.idioma()));
            traduccion.setNegocio(negocio);
            traduccion.setDescripcion(fuente.descripcion().trim());
            negocio.getTraducciones().add(traduccion);
        }
    }

    /**
     * Compone la respuesta del panel propio, con RUC y metricas.
     *
     * <p>El motivo del rechazo sale de la <strong>bitacora del Bloque 10</strong>
     * y no de una columna nueva: el administrador ya deja escrito alli por que
     * rechazo, con la entidad y su identificador, asi que el dato existe y no
     * hacia falta tocar el modelo para tenerlo.</p>
     */
    private MiNegocioResponse aRespuesta(Negocio negocio, Idioma idioma) {
        LocalDate hoy = TiempoAyacucho.hoy(clock);
        LocalDate desde = hoy.minusDays(DIAS_DE_METRICAS - 1L);

        var filas = visitasRepository.findByNegocioIdAndFechaBetweenOrderByFechaAsc(
                negocio.getId(), desde, hoy);

        List<MiNegocioResponse.DiaDeMetricas> metricas = filas.stream()
                .map(fila -> new MiNegocioResponse.DiaDeMetricas(
                        fila.getFecha(),
                        fila.getTotalVisitas(),
                        fila.getClicsWhatsapp(),
                        fila.getClicsComoLlegar()))
                .toList();

        var resumen = new MiNegocioResponse.Resumen(
                metricas.stream().mapToLong(MiNegocioResponse.DiaDeMetricas::visitas).sum(),
                metricas.stream().mapToLong(MiNegocioResponse.DiaDeMetricas::clicsWhatsapp).sum(),
                metricas.stream().mapToLong(MiNegocioResponse.DiaDeMetricas::clicsComoLlegar).sum());

        return new MiNegocioResponse(
                mapper.aRespuesta(negocio, idioma),
                negocio.getRuc(),
                motivoDelRechazo(negocio),
                metricas,
                resumen);
    }

    private String motivoDelRechazo(Negocio negocio) {
        if (negocio.getEstado() != EstadoNegocio.RECHAZADO) {
            return null;
        }
        return actividadRepository
                .findByEntidadAndEntidadIdOrderByCreatedAtDesc("Negocio", negocio.getId())
                .stream()
                .filter(registro -> "RECHAZAR_NEGOCIO".equals(registro.getAccion()))
                .findFirst()
                .map(registro -> extraerMotivo(registro.getDetalles()))
                .orElse(null);
    }

    /** Saca el valor de "motivo" del JSON de detalles, sin traer un parser. */
    private String extraerMotivo(String detalles) {
        if (detalles == null) {
            return null;
        }
        var buscador = Pattern.compile("\"motivo\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                .matcher(detalles);
        return buscador.find() ? buscador.group(1).replace("\\\"", "\"") : null;
    }
}
