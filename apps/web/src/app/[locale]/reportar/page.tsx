import { getTranslations, setRequestLocale } from "next-intl/server";

import { tiposDeIncidente } from "@/lib/reportes";

import { FormularioReporte } from "./FormularioReporte";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "reportar" });
  return { title: t("titulo"), description: t("descripcion") };
}

/**
 * Denunciar un dano al patrimonio (RF-69).
 *
 * <p>Los tipos de incidente se resuelven en el servidor con ISR: son siete y
 * cambian una vez cada varios anios, asi que el formulario aparece con ellos ya
 * pintados y no hay una espera antes de poder elegir. Cada segundo cuenta para
 * el requisito de completarlo en menos de un minuto.</p>
 */
export const revalidate = 3600;

export default async function PaginaReportar({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("reportar");
  const tipos = await tiposDeIncidente(locale);

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-8 px-5 py-10">
      <header className="flex flex-col gap-2">
        <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
        <p className="rounded-card bg-surface-muted p-4 text-fluid-sm text-text-muted">
          {t("promesaAnonimato")}
        </p>
      </header>

      <FormularioReporte tipos={tipos} />
    </main>
  );
}
