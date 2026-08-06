import { getTranslations, setRequestLocale } from "next-intl/server";

import { ListaFavoritos } from "./ListaFavoritos";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "participacion" });
  return { title: t("misFavoritos") };
}

/** Lugares guardados (RF-35). Se leen del backend, no del navegador. */
export default async function PaginaFavoritos({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("participacion");

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-5xl flex-col gap-8 px-5 py-12">
      <h1 className="text-fluid-2xl font-bold text-text">{t("misFavoritos")}</h1>
      <ListaFavoritos />
    </main>
  );
}
