import { getTranslations, setRequestLocale } from "next-intl/server";

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

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-8 px-5 py-12">
      <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
      <PanelSesion />
    </main>
  );
}
