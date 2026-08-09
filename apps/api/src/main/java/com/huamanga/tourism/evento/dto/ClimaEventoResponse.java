package com.huamanga.tourism.evento.dto;

import com.huamanga.tourism.clima.dto.PronosticoResponse;
import com.huamanga.tourism.evento.domain.Temporada;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * El clima de un evento (RF-88), con los limites dichos de frente.
 *
 * <p><strong>El problema.</strong> El plan gratuito de OpenWeatherMap alcanza
 * cinco dias. La mayoria de los eventos de una agenda cultural estan mas lejos
 * que eso, asi que <em>no tener pronostico es el caso normal</em>, no un fallo.
 * Tratarlo como un error —un 404, un hueco, un mensaje rojo— seria describir mal
 * la realidad: no es que el pronostico haya fallado, es que todavia no
 * existe.</p>
 *
 * <p>Por eso la respuesta lleva un <strong>estado explicito</strong> en vez de
 * un campo nulo que el frontend tenga que interpretar. Cada estado tiene su
 * propia forma de mostrarse, y ninguna es un error.</p>
 */
@Schema(description = "Clima de un evento, o el motivo por el que aun no lo hay")
public record ClimaEventoResponse(

        @Schema(description = """
                PRONOSTICO: el evento cae dentro de la ventana y hay datos reales.
                FUERA_DE_ALCANCE: falta mas de lo que alcanza el pronostico; se informa la temporada.
                NO_DISPONIBLE: el proveedor no responde y no hay ni dato antiguo.
                PASADO: el evento ya ocurrio; no se muestra clima.
                """)
        Estado estado,

        @Schema(description = "Solo en PRONOSTICO")
        PronosticoResponse.PronosticoDia dia,

        @Schema(description = "Temporada del mes del evento; ausente si ya paso")
        Temporada temporada,

        @Schema(description = "Dias que faltan; 0 si el evento esta ocurriendo")
        long diasParaElEvento
) {

    public enum Estado { PRONOSTICO, FUERA_DE_ALCANCE, NO_DISPONIBLE, PASADO }

    public static ClimaEventoResponse conPronostico(PronosticoResponse.PronosticoDia dia,
                                                    Temporada temporada, long dias) {
        return new ClimaEventoResponse(Estado.PRONOSTICO, dia, temporada, dias);
    }

    public static ClimaEventoResponse fueraDeAlcance(Temporada temporada, long dias) {
        return new ClimaEventoResponse(Estado.FUERA_DE_ALCANCE, null, temporada, dias);
    }

    public static ClimaEventoResponse noDisponible(Temporada temporada, long dias) {
        return new ClimaEventoResponse(Estado.NO_DISPONIBLE, null, temporada, dias);
    }

    public static ClimaEventoResponse pasado() {
        return new ClimaEventoResponse(Estado.PASADO, null, null, 0);
    }
}
