import { useFormatter, useTranslations } from "next-intl";

import type { ClimaEvento } from "@/types/evento";

/**
 * El clima de un evento, o lo que se pueda decir en su lugar (RF-88).
 *
 * <p><strong>Aqui no hay ninguna rama de error, y es a proposito.</strong> El
 * pronostico gratuito llega a cinco dias y casi todas las festividades estan
 * mas lejos, asi que quedarse sin pronostico es lo normal. Pintarlo en rojo, o
 * con un hueco lleno de guiones, describiria mal lo que pasa: el pronostico no
 * ha fallado, es que todavia no existe.</p>
 *
 * <p>Las cuatro salidas:</p>
 * <ul>
 *   <li><strong>Pronostico:</strong> minima, maxima, condicion y lluvia.</li>
 *   <li><strong>Aun lejos:</strong> una linea discreta con la temporada, que es
 *       informacion cierta y util para quien planifica un viaje.</li>
 *   <li><strong>Proveedor caido:</strong> se dice, sin fingir un dato.</li>
 *   <li><strong>Evento pasado:</strong> no se pinta nada.</li>
 * </ul>
 */
export function ClimaDelEvento({ clima }: { clima: ClimaEvento }) {
  const t = useTranslations("agenda");
  // Los consejos son los mismos del Bloque 5 y sus textos ya estan traducidos
  // en el namespace del clima: no se duplican aqui.
  const tc = useTranslations("clima");
  const formato = useFormatter();

  if (clima.estado === "PASADO") {
    return null;
  }

  if (clima.estado === "PRONOSTICO" && clima.dia) {
    const dia = clima.dia;
    return (
      <section
        data-testid="clima-evento"
        data-estado="PRONOSTICO"
        className="flex flex-col gap-2 rounded-card border border-border-base bg-surface p-4"
      >
        <h3 className="text-fluid-sm font-medium text-text-muted">{t("climaTitulo")}</h3>
        <div className="flex flex-wrap items-baseline gap-3">
          <p className="text-fluid-2xl font-bold text-text" data-testid="clima-temperaturas">
            {dia.minima !== null ? formato.number(dia.minima, { maximumFractionDigits: 0 }) : "?"}
            {" – "}
            {dia.maxima !== null ? formato.number(dia.maxima, { maximumFractionDigits: 0 }) : "?"}
            <span className="text-fluid-base font-medium text-text-muted">°C</span>
          </p>
          {dia.probabilidadLluvia !== null && dia.probabilidadLluvia > 0.2 && (
            <span className="text-fluid-sm text-text-muted">
              {t("probabilidadLluvia", {
                porcentaje: Math.round(dia.probabilidadLluvia * 100),
              })}
            </span>
          )}
        </div>
        {dia.consejos.length > 0 && (
          <ul className="flex flex-col gap-1 text-fluid-sm text-text-muted">
            {dia.consejos.map((consejo) => (
              <li key={consejo}>{tc(`consejo.${consejo}`)}</li>
            ))}
          </ul>
        )}
      </section>
    );
  }

  // Aun lejos, o el proveedor no responde. En ambos casos, una linea sobria:
  // ocupa poco, no alarma, y dice algo verdadero.
  return (
    <p
      data-testid="clima-evento"
      data-estado={clima.estado}
      className="text-fluid-sm text-text-muted"
    >
      {clima.estado === "NO_DISPONIBLE"
        ? t("climaNoDisponible")
        : t("climaAunLejos", {
            dias: clima.diasParaElEvento,
            temporada: t(`temporada.${clima.temporada ?? "SECA"}`),
          })}
    </p>
  );
}
