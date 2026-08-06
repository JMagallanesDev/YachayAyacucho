package com.huamanga.tourism.clima.service;

import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consejos automaticos segun el clima (RF-27).
 *
 * <p>Devuelve <strong>claves de traduccion, no frases</strong>. Si el backend
 * montara el texto, quedaria fijado en el idioma de la primera peticion que
 * llenara la cache y un visitante ingles recibiria consejos en espanol. La
 * frase la compone el navegador con next-intl.</p>
 *
 * <p>Los consejos son especificos de Huamanga y de su altitud (2 761 m), no
 * genericos: a esa altura la radiacion ultravioleta es notablemente mas alta y
 * la amplitud termica entre el dia y la noche es grande.</p>
 */
@Component
public class ConsejosClima {

    /** A partir de aqui, en altura, conviene cubrirse. */
    private static final double TEMPERATURA_CALOR = 22;
    private static final double TEMPERATURA_FRIO = 10;
    private static final double VIENTO_FUERTE_MS = 8;

    public List<String> paraAhora(String condicion, Double temperatura, Double viento, LocalTime hora) {
        List<String> consejos = new ArrayList<>(paraDia(condicion, temperatura, temperatura));

        if (viento != null && viento >= VIENTO_FUERTE_MS) {
            consejos.add("vientoFuerte");
        }

        // El sol de altura pega mas fuerte en las horas centrales aunque la
        // temperatura no sea alta: es radiacion, no calor.
        if (hora != null && hora.getHour() >= 10 && hora.getHour() <= 15 && esDespejado(condicion)) {
            consejos.add("uvAlto");
        }

        if (hora != null && hora.getHour() >= 17) {
            consejos.add("atardecerMirador");
        }

        return List.copyOf(consejos);
    }

    public List<String> paraDia(String condicion, Double minima, Double maxima) {
        List<String> consejos = new ArrayList<>();

        if (esLluvia(condicion)) {
            consejos.add("lluvia");
        }
        if (esTormenta(condicion)) {
            consejos.add("tormenta");
        }
        if (minima != null && minima <= TEMPERATURA_FRIO) {
            consejos.add("frioMadrugada");
        }
        if (maxima != null && maxima >= TEMPERATURA_CALOR) {
            consejos.add("hidratacion");
        }
        if (esDespejado(condicion)) {
            consejos.add("protectorSolar");
        }

        return List.copyOf(consejos);
    }

    public boolean esLluvia(String condicion) {
        return condicion != null
                && (condicion.equalsIgnoreCase("Rain")
                || condicion.equalsIgnoreCase("Drizzle")
                || condicion.equalsIgnoreCase("Thunderstorm"));
    }

    public boolean esTormenta(String condicion) {
        return "Thunderstorm".equalsIgnoreCase(condicion);
    }

    public boolean esDespejado(String condicion) {
        return "Clear".equalsIgnoreCase(condicion);
    }
}
