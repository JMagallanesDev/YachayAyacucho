import { useLocale, useTranslations } from "next-intl";

import { colorDeTipo } from "@/components/agenda/TarjetaEvento";
import { ocurreEn, rejillaDelMes } from "@/lib/fechas";
import type { Evento } from "@/types/evento";

import { MarcaDeHoy } from "./MarcaDeHoy";

/**
 * Rejilla del calendario mensual (RF-79).
 *
 * <p>Server Component sin una linea de JavaScript. La rejilla se construye con
 * aritmetica sobre {@code Date.UTC} —ver {@code lib/fechas}— de modo que ningun
 * huso horario puede correrla un dia.</p>
 *
 * <p>Un evento de varios dias pinta un punto en <strong>cada uno</strong> de sus
 * dias, que es lo que pidio el requisito. No se dibujan barras que se extiendan
 * por la rejilla: en una pantalla de movil, con siete columnas estrechas, una
 * barra a caballo entre dos filas se lee peor que un punto por dia, y cuesta
 * mucho mas CSS del que justifica.</p>
 */
export function RejillaMes({
  anio,
  mes,
  eventos,
}: {
  anio: number;
  mes: number;
  eventos: Evento[];
}) {
  const t = useTranslations("agenda");
  const idioma = useLocale();

  const casillas = rejillaDelMes(anio, mes);

  // Los nombres de los dias salen de Intl y no de un array escrito a mano: asi
  // se traducen solos y respetan las convenciones de cada idioma.
  const nombresDia = Array.from({ length: 7 }, (_, i) => {
    // 2024-01-01 fue lunes; sirve de ancla para sacar los siete nombres.
    const dia = new Date(Date.UTC(2024, 0, 1 + i));
    return new Intl.DateTimeFormat(idioma, { weekday: "narrow", timeZone: "UTC" }).format(dia);
  });

  return (
    <div className="flex flex-col gap-2" data-testid="rejilla-calendario">
      <div className="grid grid-cols-7 gap-1" aria-hidden="true">
        {nombresDia.map((nombre, i) => (
          <span
            key={i}
            className="py-1 text-center text-fluid-sm font-medium uppercase text-text-muted"
          >
            {nombre}
          </span>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1">
        {casillas.map((casilla) => {
          const delDia = eventos.filter((evento) =>
            ocurreEn(casilla.fecha, evento.fechaInicio, evento.fechaFin),
          );

          return (
            <div
              key={casilla.fecha}
              data-testid="casilla-calendario"
              data-fecha={casilla.fecha}
              data-eventos={delDia.length}
              className={`relative flex min-h-touch flex-col items-center justify-start gap-1 rounded-card p-1 ${
                casilla.delMes ? "bg-surface" : "bg-transparent"
              }`}
            >
              <MarcaDeHoy fecha={casilla.fecha} />
              {/* Los dias de los meses vecinos NO se atenuan con opacidad: al
                  70% el contraste bajaba a 3.63:1 y no llegaba al 4.5:1 (WCAG
                  1.4.3). La distincion visual ya la da el fondo de la casilla
                  —`bg-surface` frente a transparente—, que no es texto y por
                  tanto no tiene ese requisito. */}
              <span
                className={`text-fluid-sm ${
                  casilla.delMes ? "text-text" : "text-text-muted"
                }`}
              >
                {casilla.dia}
              </span>

              {delDia.length > 0 && (
                <span
                  className="flex flex-wrap justify-center gap-0.5"
                  role="img"
                  aria-label={t("eventosEseDia", { total: delDia.length })}
                >
                  {/* Como mucho tres puntos: mas no se distinguen en una casilla
                      de movil, y el listado de abajo tiene el detalle completo. */}
                  {delDia.slice(0, 3).map((evento) => (
                    <span
                      key={evento.id}
                      className="size-1.5 rounded-full"
                      style={{ backgroundColor: colorDeTipo(evento.tipo) }}
                    />
                  ))}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
