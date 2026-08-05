import { notFound } from "next/navigation";
import { getTranslations, setRequestLocale } from "next-intl/server";

import { AntesDeIr } from "@/components/lugares/AntesDeIr";
import { BadgeApertura } from "@/components/lugares/BadgeApertura";
import { GaleriaInmersiva } from "@/components/lugares/GaleriaInmersiva";
import { TablaHorarios } from "@/components/lugares/TablaHorarios";
import { routing } from "@/i18n/routing";
import { Link } from "@/i18n/navegacion";
import { obtenerLugar, slugsPublicados } from "@/lib/lugares";

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
    openGraph: {
      title: lugar.nombre,
      description: lugar.descripcion ?? undefined,
      images: lugar.fotos.slice(0, 1).map((foto) => foto.url),
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

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-3xl flex-col gap-8 px-5 py-8">
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

        <h1 data-testid="titulo-lugar" className="text-fluid-3xl font-bold text-text">
          {lugar.nombre}
        </h1>

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

      <GaleriaInmersiva
        titulo={lugar.nombre}
        imagenes={lugar.fotos.map((foto) => ({
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
