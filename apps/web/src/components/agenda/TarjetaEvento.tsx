import { useLocale, useTranslations } from "next-intl";

import { CuentaRegresiva } from "@/components/agenda/CuentaRegresiva";
import { Link } from "@/i18n/navegacion";
import { diasHasta, formatearRango } from "@/lib/fechas";
import type { Evento } from "@/types/evento";

/** Color de cada tipo de evento, con los tokens de la paleta Ayacucho. */
const COLOR_POR_TIPO: Record<string, string> = {
  RELIGIOSO: "var(--color-anil-800)",
  CIVICO: "var(--color-retablo-600)",
  CULTURAL: "var(--color-quinua-500)",
  GASTRONOMICO: "var(--color-retablo-600)",
  ARTESANAL: "var(--color-quinua-500)",
  MUSICAL: "var(--color-anil-800)",
  OTRO: "var(--color-puna-600)",
};

export function colorDeTipo(tipo: string): string {
  return COLOR_POR_TIPO[tipo] ?? COLOR_POR_TIPO.OTRO;
}

/**
 * Un evento en un listado (portada, calendario, "durante mi visita").
 *
 * <p>Server Component: nada aqui necesita JavaScript salvo la cuenta regresiva,
 * que es la unica hoja de cliente.</p>
 */
export function TarjetaEvento({
  evento,
  conCuentaRegresiva = false,
}: {
  evento: Evento;
  conCuentaRegresiva?: boolean;
}) {
  const t = useTranslations("agenda");
  const idioma = useLocale();

  return (
    <li
      data-testid="tarjeta-evento"
      data-evento-id={evento.id}
      data-tipo={evento.tipo}
      data-inicio={evento.fechaInicio}
      data-fin={evento.fechaFin}
      className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-4 shadow-card"
    >
      <div className="flex flex-wrap items-center gap-2">
        <span
          className="rounded-full px-2.5 py-1 text-fluid-sm font-medium text-text"
          style={{
            backgroundColor: `color-mix(in oklab, ${colorDeTipo(evento.tipo)} 18%, transparent)`,
          }}
        >
          {t(`tipo.${evento.tipo}`)}
        </span>
        {evento.duracionDias > 1 && (
          <span className="rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted">
            {t("duracionDias", { dias: evento.duracionDias })}
          </span>
        )}
        {conCuentaRegresiva && (
          <CuentaRegresiva
            fechaInicio={evento.fechaInicio}
            fechaFin={evento.fechaFin}
            diasIniciales={diasHasta(evento.fechaInicio)}
          />
        )}
      </div>

      <Link
        href={`/agenda/${evento.id}`}
        className="press text-fluid-lg font-semibold text-text underline-offset-4 hover:underline"
      >
        {evento.nombre}
      </Link>

      {/* La fecha se formatea con la zona fijada en UTC: el dia que llego del
          servidor es el dia que se pinta, se mire desde donde se mire. */}
      <p className="text-fluid-sm text-text-muted" data-testid="fecha-evento">
        {formatearRango(evento.fechaInicio, evento.fechaFin, idioma)}
      </p>

      {evento.lugarNombre && (
        <p className="text-fluid-sm text-text-muted">
          {evento.lugarNombre} · {evento.distritoNombre}
        </p>
      )}
    </li>
  );
}
