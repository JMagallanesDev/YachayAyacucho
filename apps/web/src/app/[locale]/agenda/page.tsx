import { getTranslations, setRequestLocale } from "next-intl/server";

import { TarjetaEvento } from "@/components/agenda/TarjetaEvento";
import { Link } from "@/i18n/navegacion";
import { eventosDelMes } from "@/lib/eventos";
import { formatearFecha, fechaISO, hoyEnAyacucho, mesVecino } from "@/lib/fechas";
import type { TipoEvento } from "@/types/evento";

import { RejillaMes } from "./RejillaMes";

const TIPOS: TipoEvento[] = [
  "RELIGIOSO",
  "CIVICO",
  "CULTURAL",
  "GASTRONOMICO",
  "ARTESANAL",
  "MUSICAL",
  "OTRO",
];

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "agenda" });
  return { title: t("titulo"), description: t("descripcion") };
}

/**
 * Calendario cultural mensual (RF-79, RF-85).
 *
 * <p><strong>El mes y el filtro viven en la URL</strong>, igual que los filtros
 * del listado de lugares del Bloque 4. La consecuencia practica es que los
 * botones de mes anterior y siguiente son enlaces: funcionan sin JavaScript, la
 * vista se puede compartir tal cual, y el boton de atras del navegador hace lo
 * que uno espera. Un calendario con el mes en el estado de React no tendria
 * ninguna de las tres cosas.</p>
 *
 * <p>Bajo la rejilla va el mismo contenido como lista. No es redundancia: una
 * rejilla de puntos no dice nada a un lector de pantalla ni a un buscador, y en
 * un movil la lista es lo que de verdad se lee.</p>
 *
 * <p>Leer {@code searchParams} hace que la pagina se renderice por peticion, no
 * que se consulte el API en cada visita: quien tiene la cache de 5 minutos es la
 * llamada de {@code eventosDelMes}. El {@code revalidate} de aqui fija ese techo
 * para todo el segmento.</p>
 */
export const revalidate = 300;

export default async function PaginaAgenda({
  params,
  searchParams,
}: {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ anio?: string; mes?: string; tipo?: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("agenda");
  const filtros = await searchParams;

  // Sin parametros se muestra el mes en curso EN AYACUCHO. Que la pagina sea
  // ISR no afecta: el mes cambia doce veces al ano y la cache dura cinco
  // minutos, asi que como mucho el dia 1 alguien ve el mes anterior un rato.
  const hoy = hoyEnAyacucho();
  const anio = Number(filtros.anio) || Number(hoy.slice(0, 4));
  const mes = Number(filtros.mes) || Number(hoy.slice(5, 7));

  const tipo = TIPOS.includes(filtros.tipo as TipoEvento) ? filtros.tipo : undefined;

  const eventos = await eventosDelMes(anio, mes, locale, tipo);

  const anterior = mesVecino(anio, mes, -1);
  const siguiente = mesVecino(anio, mes, 1);
  const enlaceMes = (destino: { anio: number; mes: number }) => {
    const parametros = new URLSearchParams({
      anio: String(destino.anio),
      mes: String(destino.mes),
    });
    if (tipo) {
      parametros.set("tipo", tipo);
    }
    return `/agenda?${parametros}`;
  };

  const nombreDelMes = formatearFecha(fechaISO(anio, mes, 1), locale, {
    month: "long",
    year: "numeric",
  });

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-3xl flex-col gap-8 px-5 py-10">
      <header className="flex flex-col gap-3">
        <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
        <Link
          href="/agenda/durante-mi-visita"
          className="press min-h-touch w-fit rounded-card bg-primary px-5 py-2 text-fluid-sm font-medium text-primary-fg"
        >
          {t("duranteMiVisita")}
        </Link>
      </header>

      {/* ---- Navegacion de mes: enlaces, no botones ------------------- */}
      <nav className="flex items-center justify-between gap-3" aria-label={t("navegarMeses")}>
        <Link
          href={enlaceMes(anterior)}
          data-testid="mes-anterior"
          className="press min-h-touch rounded-card border border-border-strong px-4 py-2 text-fluid-sm font-medium text-text"
        >
          {t("mesAnterior")}
        </Link>

        <h2
          className="text-fluid-lg font-semibold capitalize text-text"
          data-testid="mes-actual"
          data-anio={anio}
          data-mes={mes}
        >
          {nombreDelMes}
        </h2>

        <Link
          href={enlaceMes(siguiente)}
          data-testid="mes-siguiente"
          className="press min-h-touch rounded-card border border-border-strong px-4 py-2 text-fluid-sm font-medium text-text"
        >
          {t("mesSiguiente")}
        </Link>
      </nav>

      {/* ---- Filtro por tipo (RF-85), tambien en la URL --------------- */}
      <div className="flex flex-wrap gap-2" data-testid="filtros-tipo">
        <Link
          href={`/agenda?anio=${anio}&mes=${mes}`}
          aria-current={tipo ? undefined : "true"}
          data-testid="filtro-TODOS"
          className={`press min-h-touch rounded-card px-3 py-1.5 text-fluid-sm font-medium ${
            tipo ? "bg-surface-muted text-text-muted" : "bg-primary text-primary-fg"
          }`}
        >
          {t("todosLosTipos")}
        </Link>
        {TIPOS.map((candidato) => (
          <Link
            key={candidato}
            href={`/agenda?anio=${anio}&mes=${mes}&tipo=${candidato}`}
            aria-current={tipo === candidato ? "true" : undefined}
            data-testid={`filtro-${candidato}`}
            className={`press min-h-touch rounded-card px-3 py-1.5 text-fluid-sm font-medium ${
              tipo === candidato ? "bg-primary text-primary-fg" : "bg-surface-muted text-text-muted"
            }`}
          >
            {t(`tipo.${candidato}`)}
          </Link>
        ))}
      </div>

      <RejillaMes anio={anio} mes={mes} eventos={eventos} />

      <section className="flex flex-col gap-3">
        <h2 className="text-fluid-xl font-semibold text-text">
          {t("eventosDelMes", { total: eventos.length })}
        </h2>

        {eventos.length === 0 ? (
          <p data-testid="sin-eventos" className="rounded-card bg-surface-muted p-6 text-text-muted">
            {t("sinEventos")}
          </p>
        ) : (
          <ul className="flex flex-col gap-3" data-testid="lista-eventos">
            {eventos.map((evento) => (
              <TarjetaEvento key={evento.id} evento={evento} />
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
