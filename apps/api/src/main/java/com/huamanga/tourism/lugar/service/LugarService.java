package com.huamanga.tourism.lugar.service;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.common.exception.RecursoNoEncontradoException;
import com.huamanga.tourism.common.seguridad.UsuarioActual;
import com.huamanga.tourism.common.tiempo.TiempoAyacucho;
import com.huamanga.tourism.foto.domain.EstadoFoto;
import com.huamanga.tourism.foto.domain.Foto;
import com.huamanga.tourism.foto.repository.FotoRepository;
import com.huamanga.tourism.geografia.repository.DistritoRepository;
import com.huamanga.tourism.horario.domain.HorarioLugar;
import com.huamanga.tourism.lugar.domain.EstadisticaLugar;
import com.huamanga.tourism.lugar.domain.EstadoLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.domain.OrdenLugares;
import com.huamanga.tourism.lugar.repository.EstadisticaLugarRepository;
import com.huamanga.tourism.lugar.domain.LugarTraduccion;
import com.huamanga.tourism.lugar.domain.LugarTraduccionId;
import com.huamanga.tourism.lugar.dto.GeoJsonResponse;
import com.huamanga.tourism.lugar.dto.HorarioRequest;
import com.huamanga.tourism.lugar.dto.LugarDetalleResponse;
import com.huamanga.tourism.lugar.dto.LugarRequest;
import com.huamanga.tourism.lugar.dto.LugarResumenResponse;
import com.huamanga.tourism.lugar.dto.LugarTraduccionRequest;
import com.huamanga.tourism.lugar.evento.LugarGuardadoEvent;
import com.huamanga.tourism.lugar.mapper.LugarMapper;
import com.huamanga.tourism.lugar.repository.CategoriaLugarRepository;
import com.huamanga.tourism.lugar.repository.LugarRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reglas de negocio de los lugares patrimoniales (RF-47).
 */
@Service
public class LugarService {

    private static final ZoneId ZONA_AYACUCHO = TiempoAyacucho.ZONA;

    /** SRID 4326 = WGS84, el sistema de coordenadas del GPS. */
    private static final GeometryFactory GEOMETRIAS =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final LugarRepository lugarRepository;
    private final CategoriaLugarRepository categoriaRepository;
    private final DistritoRepository distritoRepository;
    private final EstadisticaLugarRepository estadisticaRepository;
    private final FotoRepository fotoRepository;
    private final LugarMapper mapper;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public LugarService(LugarRepository lugarRepository,
                        CategoriaLugarRepository categoriaRepository,
                        DistritoRepository distritoRepository,
                        EstadisticaLugarRepository estadisticaRepository,
                        FotoRepository fotoRepository,
                        LugarMapper mapper,
                        ApplicationEventPublisher eventos,
                        Clock clock) {
        this.lugarRepository = lugarRepository;
        this.categoriaRepository = categoriaRepository;
        this.distritoRepository = distritoRepository;
        this.estadisticaRepository = estadisticaRepository;
        this.fotoRepository = fotoRepository;
        this.mapper = mapper;
        this.eventos = eventos;
        this.clock = clock;
    }

    // ---------------------------------------------------------------
    //  Lectura
    // ---------------------------------------------------------------

    /**
     * Buscar, filtrar y ordenar lugares publicados
     * (RF-01, RF-02, RF-04, RF-05, RF-06).
     *
     * <p>Las estadisticas de la pagina se traen en <strong>una sola
     * consulta</strong> por identificadores y se cruzan en memoria. Pedirlas
     * una a una seria el problema N+1 clasico: una pagina de 20 tarjetas
     * costaria 21 viajes a la base solo para pintar las estrellas.</p>
     */
    @Transactional(readOnly = true)
    public Page<LugarResumenResponse> explorar(CriteriosBusqueda criterios, Idioma idioma, Pageable pagina) {
        Page<Lugar> lugares = lugarRepository.explorar(
                criterios.terminoNormalizado(),
                criterios.categoriaId(),
                criterios.calificacionMinima(),
                criterios.orden().codigo(),
                pagina);

        Map<UUID, EstadisticaLugar> estadisticas = estadisticasDe(lugares.getContent());

        return lugares.map(lugar -> mapper.aResumen(lugar, idioma, estadisticas.get(lugar.getId())));
    }

    private Map<UUID, EstadisticaLugar> estadisticasDe(List<Lugar> lugares) {
        if (lugares.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = lugares.stream().map(Lugar::getId).toList();
        return estadisticaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(EstadisticaLugar::getLugarId, e -> e));
    }

    /**
     * Criterios del explorador. Todos opcionales: sin ninguno, devuelve el
     * catalogo completo ordenado alfabeticamente.
     */
    public record CriteriosBusqueda(
            String termino,
            UUID categoriaId,
            BigDecimal calificacionMinima,
            OrdenLugares orden) {

        public CriteriosBusqueda {
            orden = orden == null ? OrdenLugares.ALFABETICO : orden;
        }

        /**
         * Un termino en blanco equivale a no buscar.
         *
         * <p>Sin esta normalizacion, borrar el buscador dejaria una cadena
         * vacia que la consulta trataria como filtro y no devolveria nada.</p>
         */
        String terminoNormalizado() {
            return termino == null || termino.isBlank() ? null : termino.trim();
        }
    }

    /**
     * Todos los lugares publicados en GeoJSON, para el mapa (RF-17, RF-18).
     *
     * <p>No se pagina: MapLibre necesita el conjunto completo para agrupar en
     * clusters correctamente. Paginarlo haria que al alejarse el mapa mostrara
     * numeros falsos, porque contaria solo la pagina cargada. Son unas decenas
     * de puntos con cuatro campos cada uno; el peso es despreciable comparado
     * con la primera tanda de tiles.</p>
     */
    @Transactional(readOnly = true)
    public GeoJsonResponse paraMapa(Idioma idioma) {
        List<GeoJsonResponse.Feature> puntos = lugarRepository
                .explorar(null, null, null, OrdenLugares.ALFABETICO.codigo(),
                        Pageable.unpaged())
                .getContent().stream()
                .map(lugar -> aPunto(lugar, idioma))
                .toList();

        return GeoJsonResponse.de(puntos);
    }

    private GeoJsonResponse.Feature aPunto(Lugar lugar, Idioma idioma) {
        var categoria = lugar.getCategoria();

        String nombreLugar = mapper.traduccionPara(lugar, idioma)
                .map(t -> t.getNombre())
                .orElse(lugar.getSlug());

        String nombreCategoria = categoria.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> categoria.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst())
                .map(t -> t.getNombre())
                .orElse(categoria.getCodigo());

        return GeoJsonResponse.Feature.punto(
                lugar.getUbicacion().getX(),
                lugar.getUbicacion().getY(),
                new GeoJsonResponse.Propiedades(
                        lugar.getId().toString(),
                        lugar.getSlug(),
                        nombreLugar,
                        categoria.getCodigo(),
                        nombreCategoria,
                        categoria.getColorHex(),
                        categoria.getIcono()));
    }

    /**
     * Ficha completa por slug.
     *
     * <p>Un lugar en BORRADOR solo existe para un administrador: para
     * cualquier otro devuelve 404, no 403. Decir "existe pero no puedes
     * verlo" confirmaria que el recurso existe, y un borrador es justamente
     * lo que aun no debe saberse que existe.</p>
     */
    @Transactional(readOnly = true)
    public LugarDetalleResponse buscarPorSlug(String slug, Idioma idioma) {
        Lugar lugar = lugarRepository.findBySlugConDetalle(slug)
                .filter(candidato -> candidato.getEstado() == EstadoLugar.PUBLICADO || esAdministrador())
                .orElseThrow(() -> new RecursoNoEncontradoException("lugar", slug));

        // Las fotos se traen aparte y no con un JOIN FETCH mas: sumarlas a la
        // consulta que ya trae traducciones y horarios multiplicaria las filas
        // (producto cartesiano de tres colecciones) y Hibernate tendria que
        // paginar en memoria.
        List<Foto> fotos = fotoRepository.findByLugarIdAndEstado(lugar.getId(), EstadoFoto.APROBADA);

        return mapper.aDetalle(lugar, idioma, estaAbiertoAhora(lugar), fotos);
    }

    // ---------------------------------------------------------------
    //  Escritura (solo ADMIN, verificado en el controller)
    // ---------------------------------------------------------------

    /**
     * Crea el lugar con sus traducciones y horarios en una sola transaccion.
     */
    @Transactional
    public LugarDetalleResponse crear(LugarRequest peticion, Idioma idioma) {
        if (lugarRepository.existsBySlug(peticion.slug())) {
            throw new SlugDuplicadoException(peticion.slug());
        }

        Lugar lugar = new Lugar();
        aplicar(peticion, lugar);
        Lugar guardado = lugarRepository.save(lugar);

        publicarGuardado(guardado);
        return mapper.aDetalle(guardado, idioma, estaAbiertoAhora(guardado));
    }

    /**
     * Reemplaza por completo un lugar, incluidas traducciones y horarios.
     */
    @Transactional
    public LugarDetalleResponse actualizar(UUID id, LugarRequest peticion, Idioma idioma) {
        Lugar lugar = lugarRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("lugar", id.toString()));

        if (!lugar.getSlug().equals(peticion.slug()) && lugarRepository.existsBySlug(peticion.slug())) {
            throw new SlugDuplicadoException(peticion.slug());
        }

        aplicar(peticion, lugar);
        Lugar guardado = lugarRepository.save(lugar);

        publicarGuardado(guardado);
        return mapper.aDetalle(guardado, idioma, estaAbiertoAhora(guardado));
    }

    /**
     * Baja logica: la fila se conserva para auditoria y para poder revertir
     * un error de moderacion (RF-56).
     */
    @Transactional
    public void eliminar(UUID id) {
        Lugar lugar = lugarRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("lugar", id.toString()));

        lugar.eliminar(clock.instant());
        lugarRepository.save(lugar);

        // Tambien hay que revalidar al borrar: si no, la pagina cacheada
        // seguiria sirviendo un lugar que ya no deberia estar visible.
        publicarGuardado(lugar);
    }

    // ---------------------------------------------------------------
    //  Interno
    // ---------------------------------------------------------------

    private void aplicar(LugarRequest peticion, Lugar lugar) {
        lugar.setSlug(peticion.slug());
        lugar.setCategoria(categoriaRepository.findById(peticion.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("categoria", peticion.categoriaId().toString())));
        lugar.setDistrito(distritoRepository.findById(peticion.distritoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("distrito", peticion.distritoId().toString())));
        lugar.setUbicacion(punto(peticion.longitud(), peticion.latitud()));
        lugar.setDireccion(peticion.direccion());
        lugar.setTelefono(peticion.telefono());
        lugar.setPrecioEntradaPen(peticion.precioEntradaPen());
        lugar.setDuracionVisitaMin(peticion.duracionVisitaMin());
        lugar.setAceptaTarjeta(peticion.aceptaTarjeta());
        lugar.setTieneBanos(peticion.tieneBanos());
        lugar.setAccesibleSillaRuedas(peticion.accesibleSillaRuedas());
        lugar.setAptoNinos(peticion.aptoNinos());
        lugar.setCostoTaxiDesdePlazaPen(peticion.costoTaxiDesdePlazaPen());
        lugar.setRequiereGuia(peticion.requiereGuia());
        lugar.setEstado(peticion.estado());

        reemplazarTraducciones(peticion, lugar);
        reemplazarHorarios(peticion, lugar);
    }

    /**
     * Vacia y rellena la MISMA coleccion.
     *
     * <p>Con {@code orphanRemoval = true}, asignar un Set nuevo hace que
     * Hibernate pierda la referencia a la coleccion que estaba gestionando y
     * falle con "A collection with orphan deletion was no longer referenced".
     * Hay que mutar la existente.</p>
     */
    private void reemplazarTraducciones(LugarRequest peticion, Lugar lugar) {
        lugar.getTraducciones().clear();
        for (LugarTraduccionRequest fuente : peticion.traducciones()) {
            LugarTraduccion traduccion = new LugarTraduccion();
            traduccion.setId(new LugarTraduccionId(lugar.getId(), fuente.idioma()));
            traduccion.setLugar(lugar);
            traduccion.setNombre(fuente.nombre());
            traduccion.setDescripcion(fuente.descripcion());
            traduccion.setHistoria(fuente.historia());
            traduccion.setConsejos(fuente.consejos());
            traduccion.setUpdatedBy(UsuarioActual.id().orElse(null));
            lugar.getTraducciones().add(traduccion);
        }
    }

    private void reemplazarHorarios(LugarRequest peticion, Lugar lugar) {
        lugar.getHorarios().clear();
        for (HorarioRequest fuente : peticion.horarios()) {
            HorarioLugar horario = new HorarioLugar();
            horario.setLugar(lugar);
            horario.setDiaSemana(fuente.diaSemana());
            horario.setHoraApertura(fuente.cerrado() ? null : fuente.horaApertura());
            horario.setHoraCierre(fuente.cerrado() ? null : fuente.horaCierre());
            horario.setCerrado(fuente.cerrado());
            horario.setUpdatedBy(UsuarioActual.id().orElse(null));
            lugar.getHorarios().add(horario);
        }
    }

    /**
     * Estado abierto/cerrado calculado al momento (RF-09b).
     *
     * <p>Se resuelve en el servidor porque depende de la hora de Ayacucho, no
     * de la del dispositivo del visitante, que puede estar en cualquier huso.</p>
     */
    private boolean estaAbiertoAhora(Lugar lugar) {
        LocalDateTime ahora = LocalDateTime.ofInstant(clock.instant(), ZONA_AYACUCHO);
        // DayOfWeek va de 1 (lunes) a 7 (domingo); el modelo usa 0 = domingo.
        short dia = (short) (ahora.getDayOfWeek().getValue() % 7);

        return lugar.getHorarios().stream()
                .filter(horario -> horario.getDiaSemana() == dia)
                .anyMatch(horario -> horario.estaAbiertoA(ahora.toLocalTime()));
    }

    private boolean esAdministrador() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication() != null
                && org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void publicarGuardado(Lugar lugar) {
        // El evento se consume DESPUES del commit: ver RevalidacionListener.
        eventos.publishEvent(new LugarGuardadoEvent(lugar.getSlug()));
    }

    private Point punto(double longitud, double latitud) {
        // Orden (x, y) = (longitud, latitud). Invertirlo es el error clasico
        // en geoespacial y colocaria Huamanga en mitad del oceano.
        Point punto = GEOMETRIAS.createPoint(new Coordinate(longitud, latitud));
        punto.setSRID(4326);
        return punto;
    }

    /** El slug forma parte de la URL publica, asi que debe ser unico. */
    public static class SlugDuplicadoException extends RuntimeException {
        public SlugDuplicadoException(String slug) {
            super("Ya existe un lugar con el slug " + slug);
        }
    }
}
