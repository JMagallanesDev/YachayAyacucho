import { cookies } from "next/headers";
import { getTranslations, setRequestLocale } from "next-intl/server";

import { ClimaDelEvento } from "@/components/agenda/ClimaDelEvento";
import { TarjetaEvento } from "@/components/agenda/TarjetaEvento";
import { Link } from "@/i18n/navegacion";
import { visita } from "@/lib/eventos";
import { formatearFecha } from "@/lib/fechas";
import { COOKIE_VIAJE, leerRangoDeViaje } from "@/lib/viaje";

import { SelectorFechasViaje } from "./SelectorFechasViaje";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "agenda" });
  return { title: t("duranteMiVisita") };
}

/**
 * "Durante mi visita" (RF-84b).
 *
 * <p><strong>Es dinamica, no ISR</strong>, y es una excepcion consciente al
 * resto del sitio: el contenido depende de una cookie y por tanto es distinto
 * para cada visitante. A cambio, quien vuelve encuentra sus fechas puestas y la
 * pagina ya resuelta desde el primer fotograma.</p>
 *
 * <p>El {@code force-dynamic} no es decorativo: el resto de rutas de este
 * segmento tienen {@code generateStaticParams} por el idioma, y sin esta linea
 * Next servia una copia prerenderizada que ignoraba la cookie. Se veia en la
 * cabecera {@code x-nextjs-cache: HIT}, y el sintoma era que el selector
 * aparecia siempre con las fechas por defecto por mucho que se guardaran.</p>
 */
export const dynamic = "force-dynamic";

export default async function PaginaDuranteMiVisita({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("agenda");

  const guardadas = (await cookies()).get(COOKIE_VIAJE)?.value;
  const { desde, hasta } = leerRangoDeViaje(guardadas);

  const resultado = await visita(desde, hasta, locale);

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-3xl flex-col gap-8 px-5 py-10">
      <header className="flex flex-col gap-3">
        <Link
          href="/agenda"
          className="press w-fit text-fluid-sm font-medium text-text-muted underline-offset-4 hover:underline"
        >
          {t("volverAlCalendario")}
        </Link>
        <h1 className="text-fluid-2xl font-bold text-text">{t("duranteMiVisita")}</h1>
        <p className="text-fluid-base text-text-muted">{t("duranteMiVisitaDescripcion")}</p>
      </header>

      <SelectorFechasViaje desde={desde} hasta={hasta} />

      {resultado === null ? (
        <p data-testid="visita-sin-datos" className="rounded-card bg-surface-muted p-6 text-text-muted">
          {t("visitaSinDatos")}
        </p>
      ) : (
        <>
          <section className="flex flex-col gap-3">
            <h2 className="text-fluid-xl font-semibold text-text">
              {t("eventosEnTuViaje", { total: resultado.eventos.length })}
            </h2>

            {resultado.eventos.length === 0 ? (
              <p
                data-testid="sin-eventos-viaje"
                className="rounded-card bg-surface-muted p-6 text-text-muted"
              >
                {t("sinEventosEnElViaje")}
              </p>
            ) : (
              <ul className="flex flex-col gap-3" data-testid="eventos-viaje">
                {resultado.eventos.map((evento) => (
                  <TarjetaEvento key={evento.id} evento={evento} conCuentaRegresiva />
                ))}
              </ul>
            )}
          </section>

          {/* Dia a dia. Los primeros traeran pronostico real y los ultimos solo
              la temporada; la transicion no se explica porque no hace falta:
              cada dia dice lo que sabe. */}
          <section className="flex flex-col gap-3">
            <h2 className="text-fluid-xl font-semibold text-text">{t("diaADia")}</h2>
            <ul className="flex flex-col gap-3" data-testid="dias-viaje">
              {resultado.dias.map((dia) => (
                <li
                  key={dia.fecha}
                  data-testid="dia-viaje"
                  data-fecha={dia.fecha}
                  data-eventos={dia.eventoIds.length}
                  className="flex flex-col gap-2 rounded-card border border-border-base bg-surface p-4"
                >
                  <span className="text-fluid-base font-medium capitalize text-text">
                    {formatearFecha(dia.fecha, locale, {
                      weekday: "long",
                      day: "numeric",
                      month: "long",
                    })}
                  </span>

                  <ClimaDelEvento clima={dia.clima} />

                  <span className="text-fluid-sm text-text-muted">
                    {dia.eventoIds.length === 0
                      ? t("sinEventosEseDia")
                      : t("eventosEseDia", { total: dia.eventoIds.length })}
                  </span>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}
    </main>
  );
}
