import { getTranslations, setRequestLocale } from "next-intl/server";
import { notFound } from "next/navigation";

import { Compartir } from "@/components/Compartir";
import { RegistrarVisita } from "@/components/RegistrarVisita";
import { DatosEstructurados, migas, organizacion } from "@/components/seo/DatosEstructurados";
import { ClimaDelEvento } from "@/components/agenda/ClimaDelEvento";
import { CuentaRegresiva } from "@/components/agenda/CuentaRegresiva";
import { colorDeTipo } from "@/components/agenda/TarjetaEvento";
import { VideoFestividad } from "@/components/agenda/VideoFestividad";
import { Link } from "@/i18n/navegacion";
import { env } from "@/lib/env";
import { eventoPorId } from "@/lib/eventos";
import { diasHasta, formatearRango } from "@/lib/fechas";
import Image from "next/image";
import { TAMANOS, cargadorCloudinary } from "@/lib/imagenes";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string; id: string }>;
}) {
  const { locale, id } = await params;
  const detalle = await eventoPorId(id, locale);
  if (!detalle) {
    return {};
  }
  return {
    title: detalle.evento.nombre,
    description: detalle.evento.descripcion ?? undefined,
  };
}

/**
 * Ficha completa de un evento (RF-80) con su clima (RF-88).
 *
 * <p>Se renderiza en el servidor con ISR de media hora. El clima viaja dentro
 * de la misma respuesta del API, asi que no hace falta ninguna llamada extra
 * desde el navegador; y media hora es tiempo suficientemente corto para que el
 * pronostico de un evento cercano no se quede rancio.</p>
 *
 * <p>La URL usa el identificador del evento y no un slug porque la tabla no
 * tiene columna de slug. Es peor para SEO y esta anotado como tal.</p>
 */
export const revalidate = 1800;

export default async function PaginaEvento({
  params,
}: {
  params: Promise<{ locale: string; id: string }>;
}) {
  const { locale, id } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("agenda");
  const detalle = await eventoPorId(id, locale);

  if (!detalle) {
    notFound();
  }

  const { evento, clima } = detalle;

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-6 px-5 py-10">
      <RegistrarVisita tipo="EVENTO" />

      {/* `Event` es de los tipos que Google muestra en su carrusel de eventos.
          Las fechas van en formato de dia de calendario, sin hora ni zona: es
          lo que son, y anadir una hora inventada las desplazaria de dia en
          otros husos. */}
      <DatosEstructurados
        datos={{
          "@context": "https://schema.org",
          "@type": "Event",
          name: evento.nombre,
          description: evento.descripcion ?? undefined,
          url: `${env.siteUrl}/${locale}/agenda/${id}`,
          startDate: evento.fechaInicio,
          endDate: evento.fechaFin,
          eventStatus: "https://schema.org/EventScheduled",
          eventAttendanceMode: "https://schema.org/OfflineEventAttendanceMode",
          image: evento.cloudinaryUrlPortada ? [evento.cloudinaryUrlPortada] : undefined,
          location: {
            "@type": "Place",
            name: evento.lugarNombre ?? evento.distritoNombre,
            address: {
              "@type": "PostalAddress",
              addressLocality: evento.distritoNombre,
              addressRegion: "Ayacucho",
              addressCountry: "PE",
            },
          },
          organizer: evento.organizador
            ? { "@type": "Organization", name: evento.organizador }
            : organizacion(),
        }}
      />
      <DatosEstructurados
        datos={migas(locale, [
          { nombre: t("volverAlCalendario"), ruta: "/agenda" },
          { nombre: evento.nombre, ruta: `/agenda/${id}` },
        ])}
      />
      <Link
        href="/agenda"
        className="press w-fit text-fluid-sm font-medium text-text-muted underline-offset-4 hover:underline"
      >
        {t("volverAlCalendario")}
      </Link>

      <header className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span
            className="rounded-full px-2.5 py-1 text-fluid-sm font-medium text-text"
            /* El fondo conserva el color del tipo; el texto usa el token
               medido. El color del catalogo no pasa por el sistema de tokens,
               asi que como TEXTO no se puede garantizar el 4.5:1 (WCAG 1.4.3):
               un ocre claro sobre su propio tinte daba 3.6:1. */
            style={{
              backgroundColor: `color-mix(in oklab, ${colorDeTipo(evento.tipo)} 18%, transparent)`,
            }}
          >
            {t(`tipo.${evento.tipo}`)}
          </span>
          {clima.estado !== "PASADO" && (
            <CuentaRegresiva
              fechaInicio={evento.fechaInicio}
              fechaFin={evento.fechaFin}
              diasIniciales={diasHasta(evento.fechaInicio)}
            />
          )}
        </div>

        <h1 className="text-fluid-2xl font-bold text-text" data-testid="nombre-evento">
          {evento.nombre}
        </h1>

        <p className="text-fluid-base font-medium text-text" data-testid="fecha-evento">
          {formatearRango(evento.fechaInicio, evento.fechaFin, locale)}
        </p>

        {evento.duracionDias > 1 && (
          <p className="text-fluid-sm text-text-muted">
            {t("duracionDias", { dias: evento.duracionDias })}
          </p>
        )}
      </header>

      {evento.cloudinaryUrlPortada && (
        <Image
          src={evento.cloudinaryUrlPortada}
          alt=""
          width={768}
          height={224}
          sizes={TAMANOS.ficha}
          loader={cargadorCloudinary}
          /* Es la imagen mas grande de la mitad superior: se marca como
             prioritaria para que sea el LCP y no espere a la carga diferida. */
          priority
          className="h-56 w-full rounded-card object-cover"
        />
      )}

      {evento.youtubeVideoId && (
        <VideoFestividad videoId={evento.youtubeVideoId} titulo={evento.nombre} />
      )}

      <ClimaDelEvento clima={clima} />

      {evento.descripcion && (
        <p className="whitespace-pre-line text-fluid-base leading-relaxed text-text">
          {evento.descripcion}
        </p>
      )}

      <dl className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-5">
        {evento.lugarNombre && (
          <div className="flex flex-col gap-0.5">
            <dt className="text-fluid-sm text-text-muted">{t("donde")}</dt>
            <dd className="text-fluid-base text-text">
              {evento.lugarSlug ? (
                <Link
                  href={`/lugares/${evento.lugarSlug}`}
                  className="press underline underline-offset-4"
                >
                  {evento.lugarNombre}
                </Link>
              ) : (
                evento.lugarNombre
              )}
            </dd>
          </div>
        )}

        <div className="flex flex-col gap-0.5">
          <dt className="text-fluid-sm text-text-muted">{t("distrito")}</dt>
          <dd className="text-fluid-base text-text">{evento.distritoNombre}</dd>
        </div>

        {evento.organizador && (
          <div className="flex flex-col gap-0.5">
            <dt className="text-fluid-sm text-text-muted">{t("organiza")}</dt>
            <dd className="text-fluid-base text-text">{evento.organizador}</dd>
          </div>
        )}
      </dl>

      <Compartir titulo={evento.nombre} />

      {evento.recurrenteAnual && (
        <p className="text-fluid-sm text-text-muted" data-testid="aviso-recurrente">
          {t("seRepiteCadaAnio")}
        </p>
      )}
    </main>
  );
}
