import { notFound } from "next/navigation";
import { getTranslations, setRequestLocale } from "next-intl/server";

import { Compartir } from "@/components/Compartir";
import { RegistrarVisita } from "@/components/RegistrarVisita";
import { DatosEstructurados, migas, organizacion } from "@/components/seo/DatosEstructurados";
import { HistoriaVisual } from "@/components/patrimonio/HistoriaVisual";
import { AntesDeIr } from "@/components/lugares/AntesDeIr";
import { BadgeApertura } from "@/components/lugares/BadgeApertura";
import { GaleriaInmersiva } from "@/components/lugares/GaleriaInmersiva";
import { TablaHorarios } from "@/components/lugares/TablaHorarios";
import { routing } from "@/i18n/routing";
import { Link } from "@/i18n/navegacion";
import { BotonCheckIn } from "@/components/participacion/BotonCheckIn";
import { BotonFavorito } from "@/components/participacion/BotonFavorito";
import { PanelResenas } from "@/components/resenas/PanelResenas";
import { SubirFoto } from "@/components/resenas/SubirFoto";
import { env } from "@/lib/env";
import { obtenerLugar, slugsPublicados } from "@/lib/lugares";
import { historiaVisual } from "@/lib/negocios";
import { listarFotos, listarResenas } from "@/lib/resenas";

/**
 * Ficha de un lugar patrimonial (RF-09).
 *
 * <p>Server Component con ISR. Todo el contenido patrimonial —historia,
 * descripcion, horarios, datos practicos— se renderiza en el servidor: es lo
 * que indexan los buscadores y lo que se ve aunque el JavaScript tarde. Solo
 * dos hojas son de cliente: la galeria, que necesita gestos, y la insignia de
 * abierto/cerrado, que depende de la hora actual y no puede quedarse congelada
 * en el HTML cacheado.</p>
 */
export const revalidate = 3600;

/**
 * Pre-genera las fichas publicadas en compilacion.
 *
 * <p>{@code dynamicParams} sigue activo por defecto: un lugar creado despues
 * del despliegue se genera bajo demanda en su primera visita y a partir de ahi
 * queda cacheado.</p>
 */
export async function generateStaticParams() {
  const slugs = await slugsPublicados();
  return routing.locales.flatMap((locale) => slugs.map((slug) => ({ locale, slug })));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string; slug: string }>;
}) {
  const { locale, slug } = await params;
  const lugar = await obtenerLugar(slug, locale);

  if (!lugar) {
    return {};
  }
  return {
    title: lugar.nombre,
    description: lugar.descripcion ?? undefined,
    alternates: {
      canonical: `/${locale}/lugares/${slug}`,
      languages: {
        es: `/es/lugares/${slug}`,
        en: `/en/lugares/${slug}`,
      },
    },
    openGraph: {
      type: "article",
      title: lugar.nombre,
      description: lugar.descripcion ?? undefined,
      url: `/${locale}/lugares/${slug}`,
      /*
       * Con foto se usa la del lugar; sin ella, la tarjeta de marca que genera
       * `opengraph-image.tsx`. Hay que nombrarla explicitamente: cuando una
       * pagina declara su propio bloque `openGraph`, Next deja de anadir la
       * imagen basada en archivo, y el enlace compartido se quedaba sin
       * miniatura justo en los lugares que aun no tienen fotografia aprobada.
       */
      images:
        lugar.fotos.length > 0
          ? lugar.fotos.slice(0, 1).map((foto) => foto.url)
          : [`${env.siteUrl}/${locale}/opengraph-image`],
    },
  };
}

export default async function PaginaLugar({
  params,
}: {
  params: Promise<{ locale: string; slug: string }>;
}) {
  const { locale, slug } = await params;
  setRequestLocale(locale);

  const lugar = await obtenerLugar(slug, locale);
  if (!lugar) {
    notFound();
  }

  const t = await getTranslations("lugares");

  // En paralelo: ninguna depende de otra.
  const [resenas, fotos, imagenesHistoricas] = await Promise.all([
    listarResenas(slug),
    listarFotos(slug),
    // Casi ningun lugar tiene foto antigua localizable: la lista suele venir
    // vacia y entonces la seccion no se pinta. Degradacion elegante, sin error.
    historiaVisual(slug),
  ]);

  // El promedio se calcula sobre las resenas visibles y se muestra junto al
  // numero de opiniones. Es la misma cifra que la vista materializada sirve al
  // listado; aqui se deriva de la pagina ya cargada para no pedir otra cosa.
  const promedio =
    resenas.content.length > 0
      ? resenas.content.reduce((suma, r) => suma + r.calificacion, 0) / resenas.content.length
      : null;

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-3xl flex-col gap-8 px-5 py-8">
      <RegistrarVisita tipo="LUGAR" />

      {/* Sin esto un buscador ve un titulo y un parrafo; con esto sabe que es
          una atraccion turistica con coordenadas, horario y valoracion, y puede
          mostrarla como resultado enriquecido o en el mapa de Google. */}
      <DatosEstructurados
        datos={{
          "@context": "https://schema.org",
          "@type": "TouristAttraction",
          name: lugar.nombre,
          description: lugar.descripcion ?? undefined,
          url: `${env.siteUrl}/${locale}/lugares/${slug}`,
          image: lugar.fotos.slice(0, 3).map((foto) => foto.url),
          geo: {
            "@type": "GeoCoordinates",
            latitude: lugar.latitud,
            longitude: lugar.longitud,
          },
          address: {
            "@type": "PostalAddress",
            streetAddress: lugar.direccion ?? undefined,
            addressLocality: lugar.distrito.nombre,
            addressRegion: lugar.distrito.provincia,
            addressCountry: "PE",
          },
          telephone: lugar.telefono ?? undefined,
          isAccessibleForFree: lugar.precioEntradaPen === null
            || Number(lugar.precioEntradaPen) === 0,
          publicAccess: true,
          // Solo se declara la valoracion si existe de verdad: un
          // `aggregateRating` con cero opiniones es un dato falso y Google lo
          // penaliza como marcado enganoso.
          ...(resenas.totalElements > 0 && promedio !== null
            ? {
                aggregateRating: {
                  "@type": "AggregateRating",
                  ratingValue: promedio.toFixed(1),
                  reviewCount: resenas.totalElements,
                  bestRating: 5,
                  worstRating: 1,
                },
              }
            : {}),
          isPartOf: organizacion(),
        }}
      />
      <DatosEstructurados
        datos={migas(locale, [
          { nombre: t("volverAlListado"), ruta: "/lugares" },
          { nombre: lugar.nombre, ruta: `/lugares/${slug}` },
        ])}
      />
      <nav>
        <Link href="/lugares" className="text-fluid-sm text-text-muted underline-offset-4 hover:underline">
          {t("volverAlListado")}
        </Link>
      </nav>

      <header className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span
            className="rounded-full bg-surface-muted px-3 py-1 text-fluid-xs font-medium text-text-muted"
            data-testid="categoria-lugar"
          >
            {lugar.categoria.nombre}
          </span>
          <BadgeApertura horarios={lugar.horarios} />
        </div>

        <div className="flex items-start justify-between gap-3">
          <h1 data-testid="titulo-lugar" className="text-fluid-3xl font-bold text-text">
            {lugar.nombre}
          </h1>
          <BotonFavorito slug={slug} />
        </div>

        <p className="text-fluid-sm text-text-muted">
          {[lugar.direccion, lugar.distrito.nombre, lugar.distrito.provincia]
            .filter(Boolean)
            .join(" · ")}
        </p>

        {/* Aviso honesto cuando la ficha aun no esta traducida al idioma
            pedido: es preferible a fingir que el contenido esta en ingles. */}
        {lugar.traduccionPorDefecto && (
          <p className="rounded-card bg-surface-muted px-4 py-3 text-fluid-sm text-text-muted">
            {t("sinTraduccion")}
          </p>
        )}
      </header>

      {promedio !== null && (
        <p data-testid="promedio-lugar" className="flex items-center gap-2 text-fluid-base">
          <span className="text-accent-text" aria-hidden="true">
            ★
          </span>
          <span className="font-semibold text-text">{promedio.toFixed(1)}</span>
          <span className="text-text-muted">
            {t("basadoEn", { total: resenas.totalElements })}
          </span>
        </p>
      )}

      {/* La galeria se alimenta de las fotos APROBADAS que suben los
          visitantes: es lo que la llena, porque el modelo no guarda imagenes
          propias del lugar. */}
      <GaleriaInmersiva
        titulo={lugar.nombre}
        imagenes={fotos.map((foto) => ({
          url: foto.url,
          alt: t("fotoDe", { titulo: lugar.nombre }),
        }))}
      />

      {lugar.descripcion && (
        <section className="flex flex-col gap-3">
          <h2 className="text-fluid-xl font-semibold text-text">{t("descripcionTitulo")}</h2>
          <p className="text-fluid-base leading-relaxed text-text">{lugar.descripcion}</p>
        </section>
      )}

      {lugar.historia && (
        <section className="flex flex-col gap-3">
          <h2 className="text-fluid-xl font-semibold text-text">{t("historia")}</h2>
          <p className="whitespace-pre-line text-fluid-base leading-relaxed text-text">
            {lugar.historia}
          </p>
        </section>
      )}

      <AntesDeIr lugar={lugar} />

      <TablaHorarios horarios={lugar.horarios} />

      {lugar.consejos && (
        <section className="flex flex-col gap-3">
          <h2 className="text-fluid-xl font-semibold text-text">{t("consejos")}</h2>
          <p className="whitespace-pre-line text-fluid-base leading-relaxed text-text">
            {lugar.consejos}
          </p>
        </section>
      )}

      <HistoriaVisual imagenes={imagenesHistoricas} />

      <BotonCheckIn slug={slug} />

      <SubirFoto slug={slug} />

      <PanelResenas slug={slug} resenasIniciales={resenas.content} />

      <Compartir titulo={lugar.nombre} />

      {lugar.telefono && (
        <p className="text-fluid-sm text-text-muted">
          {t("telefono")}:{" "}
          <a href={`tel:${lugar.telefono}`} className="text-primary underline-offset-4 hover:underline">
            {lugar.telefono}
          </a>
        </p>
      )}
    </main>
  );
}
