package com.huamanga.tourism.evento.mapper;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.evento.domain.Evento;
import com.huamanga.tourism.evento.domain.EventoTraduccion;
import com.huamanga.tourism.evento.dto.ClimaEventoResponse;
import com.huamanga.tourism.evento.dto.EventoDetalleResponse;
import com.huamanga.tourism.evento.dto.EventoResumenResponse;
import org.mapstruct.Mapper;

import java.util.Optional;

/**
 * Entidad a DTO. Los controllers nunca devuelven entidades JPA.
 *
 * <p>La eleccion de traduccion —idioma pedido, y si no, espanol— sigue el mismo
 * criterio que {@code LugarMapper}: el espanol siempre existe porque la
 * validacion lo exige al guardar, asi que el fallback nunca deja una ficha sin
 * texto.</p>
 */
@Mapper(componentModel = "spring")
public interface EventoMapper {

    default Optional<EventoTraduccion> traduccionPara(Evento evento, Idioma idioma) {
        return evento.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> evento.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst());
    }

    default EventoResumenResponse aResumen(Evento evento, Idioma idioma) {
        EventoTraduccion traduccion = traduccionPara(evento, idioma).orElseThrow(
                () -> new IllegalStateException(
                        "Evento " + evento.getId() + " sin traduccion en espanol: revisar la validacion de guardado"));

        Idioma idiomaDevuelto = traduccion.getId().getIdioma();

        return new EventoResumenResponse(
                evento.getId(),
                idiomaDevuelto,
                idiomaDevuelto != idioma,
                traduccion.getNombre(),
                traduccion.getDescripcion(),
                traduccion.getOrganizador(),
                evento.getTipo(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.duracionEnDias(),
                evento.getCloudinaryUrlPortada(),
                evento.getLugar() != null ? nombreDeLugar(evento, idioma) : null,
                evento.getLugar() != null ? evento.getLugar().getSlug() : null,
                evento.getDistrito().getNombre(),
                evento.isRecurrenteAnual(),
                evento.getEstado());
    }

    default EventoDetalleResponse aDetalle(Evento evento, Idioma idioma, ClimaEventoResponse clima) {
        return new EventoDetalleResponse(
                aResumen(evento, idioma),
                clima,
                evento.getEventoOrigen() != null ? evento.getEventoOrigen().getId() : null);
    }

    private String nombreDeLugar(Evento evento, Idioma idioma) {
        var lugar = evento.getLugar();
        return lugar.getTraducciones().stream()
                .filter(t -> t.getId().getIdioma() == idioma)
                .findFirst()
                .or(() -> lugar.getTraducciones().stream()
                        .filter(t -> t.getId().getIdioma() == Idioma.ES)
                        .findFirst())
                .map(t -> t.getNombre())
                .orElse(lugar.getSlug());
    }
}
