package com.huamanga.tourism.evento.controller;

import com.huamanga.tourism.common.domain.Idioma;
import com.huamanga.tourism.evento.domain.TipoEvento;
import com.huamanga.tourism.evento.dto.EventoDetalleResponse;
import com.huamanga.tourism.evento.dto.EventoResumenResponse;
import com.huamanga.tourism.evento.dto.VisitaResponse;
import com.huamanga.tourism.evento.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Agenda cultural: consulta publica (RF-79, RF-80, RF-84, RF-84b, RF-85).
 *
 * <p>Todas las fechas de este controlador son <strong>dias de calendario</strong>
 * en formato ISO ({@code 2027-03-21}), sin hora ni zona. Es lo que corresponde:
 * una festividad ocurre "el 5 de abril" para todo el mundo, y meterla en un
 * instante obligaria a inventar una hora y la expondria a desplazarse un dia al
 * cambiar de huso.</p>
 */
@RestController
@RequestMapping("/eventos")
@Tag(name = "Agenda cultural", description = "Calendario, fichas y cruce con las fechas del viaje")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/calendario")
    @Operation(summary = "Eventos de un mes",
            description = """
                    Devuelve los eventos publicados que tocan el mes pedido
                    (RF-79). Es una consulta de solape: una festividad de varios
                    dias aparece si cualquiera de sus dias cae en el mes, aunque
                    empiece el mes anterior.
                    """)
    public List<EventoResumenResponse> calendario(
            @Parameter(description = "Anio de cuatro cifras") @RequestParam int anio,
            @Parameter(description = "Mes, de 1 a 12") @RequestParam int mes,
            @Parameter(description = "Filtrar por tipo (RF-85)")
            @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false) Idioma idioma) {

        return eventoService.delMes(anio, mes, tipo, resolverIdioma(idioma));
    }

    @GetMapping("/proximos")
    @Operation(summary = "Proximos eventos",
            description = """
                    Para la cuenta regresiva de la portada (RF-84). Incluye los
                    que ya empezaron y aun no terminan: una fiesta en marcha
                    sigue siendo un proximo evento para quien esta en la ciudad.
                    """)
    public List<EventoResumenResponse> proximos(
            @RequestParam(defaultValue = "5") int limite,
            @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false) Idioma idioma) {

        return eventoService.proximos(limite, tipo, resolverIdioma(idioma));
    }

    @GetMapping("/durante-mi-visita")
    @Operation(summary = "Que ocurre durante un viaje",
            description = """
                    Cruza las fechas del viaje con la agenda (RF-84b) y devuelve
                    ademas el clima de cada dia. Los primeros dias traeran
                    pronostico real y los ultimos, solo la temporada: el
                    pronostico gratuito alcanza cinco dias.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dias del viaje y eventos coincidentes"),
            @ApiResponse(responseCode = "422", description = "Rango invertido o de mas de 30 dias")
    })
    public VisitaResponse duranteMiVisita(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Idioma idioma) {

        return eventoService.duranteMiVisita(desde, hasta, resolverIdioma(idioma));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha de un evento",
            description = """
                    Contenido completo (RF-80) con el clima que se pueda decir de
                    el (RF-88). Si el evento queda mas alla del pronostico, el
                    campo clima no es un error: informa de la temporada.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha del evento"),
            @ApiResponse(responseCode = "404", description = "No existe, o es borrador y quien pregunta no es administrador")
    })
    public EventoDetalleResponse detalle(
            @PathVariable UUID id,
            @RequestParam(required = false) Idioma idioma) {

        return eventoService.detalle(id, resolverIdioma(idioma));
    }

    private Idioma resolverIdioma(Idioma explicito) {
        if (explicito != null) {
            return explicito;
        }
        return "en".equals(LocaleContextHolder.getLocale().getLanguage()) ? Idioma.EN : Idioma.ES;
    }
}
