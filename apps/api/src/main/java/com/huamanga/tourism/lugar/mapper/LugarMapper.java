package com.huamanga.tourism.lugar.mapper;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.foto.domain.Foto;
import com.huamanga.tourism.horario.domain.HorarioLugar;
import com.huamanga.tourism.lugar.domain.EstadisticaLugar;
import com.huamanga.tourism.lugar.domain.Lugar;
import com.huamanga.tourism.lugar.domain.LugarTraduccion;
import com.huamanga.tourism.lugar.dto.HorarioResponse;
import com.huamanga.tourism.lugar.dto.LugarDetalleResponse;
import com.huamanga.tourism.lugar.dto.LugarResumenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Conversion de entidades a DTOs, generada en compilacion por MapStruct.
 *
 * <p>Los controllers nunca devuelven entidades JPA (regla no negociable del
 * CLAUDE.md): exponerlas filtraria la estructura interna y campos que no
 * deben salir, y ataria el contrato del API al modelo de datos.</p>
 *
 * <p>Lo que MapStruct no puede resolver solo —elegir entre un conjunto de
 * traducciones la del idioma pedido, con su fallback— va en metodos
 * {@code default} de esta misma interfaz. Es logica de presentacion, no de
 * negocio, asi que su sitio es el mapper y no el service.</p>
 */
@Mapper(componentModel = "spring")
public interface LugarMapper {

    @Named("aHorarioResponse")
    default HorarioResponse aHorarioResponse(HorarioLugar horario) {
        return new HorarioResponse(
                horario.getDiaSemana(),
                horario.getHoraApertura(),
                horario.getHoraCierre(),
                horario.isCerrado());
    }

    /**
     * Grilla semanal ordenada por dia y, dentro de cada dia, por hora de
     * apertura, para que el frontend pueda pintarla sin reordenar.
     */
    default List<HorarioResponse> aHorarios(Iterable<HorarioLugar> horarios) {
        return java.util.stream.StreamSupport.stream(horarios.spliterator(), false)
                .sorted(Comparator
                        .comparing(HorarioLugar::getDiaSemana)
                        .thenComparing(h -> h.getHoraApertura(),
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(this::aHorarioResponse)
                .toList();
    }

    /**
     * Traduccion del idioma pedido; si no existe, la espanola.
     *
     * <p>El espanol siempre esta —lo garantiza la validacion al guardar—, asi
     * que este fallback nunca deja una ficha sin texto. Es el mismo criterio
     * que el frontend aplica a frances y aleman.</p>
     */
    default Optional<LugarTraduccion> traduccionPara(Lugar lugar, Idioma idioma) {
        return lugar.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> lugar.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst());
    }

    default LugarDetalleResponse aDetalle(Lugar lugar, Idioma idioma, boolean abiertoAhora) {
        return aDetalle(lugar, idioma, abiertoAhora, List.of());
    }

    default LugarDetalleResponse aDetalle(Lugar lugar, Idioma idioma, boolean abiertoAhora,
                                          List<Foto> fotos) {
        LugarTraduccion traduccion = traduccionPara(lugar, idioma).orElseThrow(
                () -> new IllegalStateException(
                        "Lugar " + lugar.getSlug() + " sin traduccion en espanol: revisar la validacion de guardado"));

        Idioma idiomaDevuelto = traduccion.getId().getIdioma();

        return new LugarDetalleResponse(
                lugar.getId(),
                lugar.getSlug(),
                idiomaDevuelto,
                idiomaDevuelto != idioma,
                traduccion.getNombre(),
                traduccion.getDescripcion(),
                traduccion.getHistoria(),
                traduccion.getConsejos(),
                aCategoria(lugar, idioma),
                aDistrito(lugar),
                lugar.getUbicacion().getX(),
                lugar.getUbicacion().getY(),
                lugar.getDireccion(),
                lugar.getTelefono(),
                lugar.getPrecioEntradaPen(),
                lugar.getDuracionVisitaMin(),
                lugar.getAceptaTarjeta(),
                lugar.getTieneBanos(),
                lugar.getAccesibleSillaRuedas(),
                lugar.getAptoNinos(),
                lugar.getCostoTaxiDesdePlazaPen(),
                lugar.getRequiereGuia(),
                lugar.getEstado(),
                aHorarios(lugar.getHorarios()),
                abiertoAhora,
                fotos.stream()
                        .map(foto -> new LugarDetalleResponse.FotoResponse(foto.getId(), foto.getCloudinaryUrl()))
                        .toList(),
                lugar.getUpdatedAt());
    }

    /**
     * @param estadistica agregados de la vista materializada; puede faltar si
     *                    la vista aun no se ha refrescado tras crear el lugar,
     *                    en cuyo caso se devuelven ceros en vez de nulos para
     *                    que la tarjeta no tenga que defenderse de ellos
     */
    default LugarResumenResponse aResumen(Lugar lugar, Idioma idioma, EstadisticaLugar estadistica) {
        LugarTraduccion traduccion = traduccionPara(lugar, idioma).orElseThrow(
                () -> new IllegalStateException("Lugar " + lugar.getSlug() + " sin traduccion en espanol"));

        return new LugarResumenResponse(
                lugar.getId(),
                lugar.getSlug(),
                traduccion.getNombre(),
                traduccion.getDescripcion(),
                aCategoria(lugar, idioma),
                lugar.getUbicacion().getX(),
                lugar.getUbicacion().getY(),
                lugar.getPrecioEntradaPen(),
                lugar.getDuracionVisitaMin(),
                lugar.getEstado(),
                aHorarios(lugar.getHorarios()),
                estadistica != null ? estadistica.getCalificacionPromedio() : BigDecimal.ZERO,
                estadistica != null ? estadistica.getTotalResenas() : 0L,
                estadistica != null ? estadistica.getTotalVisitas() : 0L);
    }

    private LugarDetalleResponse.CategoriaResponse aCategoria(Lugar lugar, Idioma idioma) {
        var categoria = lugar.getCategoria();
        String nombre = categoria.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> categoria.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst())
                .map(t -> t.getNombre())
                .orElse(categoria.getCodigo());

        return new LugarDetalleResponse.CategoriaResponse(
                categoria.getId(), categoria.getCodigo(), nombre,
                categoria.getIcono(), categoria.getColorHex());
    }

    private LugarDetalleResponse.DistritoResponse aDistrito(Lugar lugar) {
        var distrito = lugar.getDistrito();
        return new LugarDetalleResponse.DistritoResponse(
                distrito.getId(), distrito.getCodigo(), distrito.getNombre(),
                distrito.getProvincia().getNombre());
    }
}
