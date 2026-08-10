import { getTranslations, setRequestLocale } from "next-intl/server";

import { InterruptorTema } from "@/components/tema/InterruptorTema";
import { Link } from "@/i18n/navegacion";

import { PanelSesion } from "./PanelSesion";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "perfil" });
  return { title: t("titulo") };
}

/**
 * Ruta privada. El `proxy.ts` impide llegar aqui sin cookie de sesion, pero
 * los datos se piden al backend con el access token, que es quien autoriza de
 * verdad.
 */
export default async function PaginaPerfil({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("perfil");
  const tTema = await getTranslations("tema");

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-8 px-5 py-12">
      <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
      <PanelSesion />

      {/* Accesos a las secciones que no caben en los cinco destinos de la
          barra. El perfil es su sitio natural: son cosas de "lo mio". */}
      <nav className="grid gap-2 sm:grid-cols-2" data-testid="atajos-perfil">
        {[
          { href: "/perfil/favoritos", clave: "favoritos" },
          { href: "/perfil/pasaporte", clave: "pasaporte" },
          { href: "/perfil/mi-negocio", clave: "miNegocio" },
          { href: "/negocios", clave: "directorio" },
          { href: "/mapa-incidentes", clave: "incidentes" },
          { href: "/reportar", clave: "reportar" },
        ].map((atajo) => (
          <Link
            key={atajo.href}
            href={atajo.href}
            data-testid={`atajo-${atajo.clave}`}
            className="press flex min-h-touch items-center rounded-card border border-border-base bg-surface px-4 text-fluid-base font-medium text-text"
          >
            {t(`atajo.${atajo.clave}`)}
          </Link>
        ))}
      </nav>

      {/* El interruptor de tema vive aqui en movil: en la barra inferior no
          cabe junto a cinco destinos. En escritorio esta en las dos partes. */}
      <section className="flex flex-col gap-3 md:hidden" data-testid="apariencia">
        <h2 className="text-fluid-lg font-semibold text-text">{tTema("titulo")}</h2>
        <InterruptorTema />
      </section>
    </main>
  );
}
