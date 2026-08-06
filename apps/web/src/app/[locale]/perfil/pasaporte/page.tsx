import { getTranslations, setRequestLocale } from "next-intl/server";

import { Pasaporte } from "./Pasaporte";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "pasaporte" });
  return { title: t("titulo") };
}

/**
 * Pasaporte patrimonial (RF-39b).
 *
 * <p>El contenido es de cliente porque depende de la sesion, que vive en
 * memoria; el `proxy.ts` ya impide llegar aqui sin cookie, y el backend vuelve
 * a comprobarlo en cada llamada.</p>
 */
export default async function PaginaPasaporte({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("pasaporte");

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-3xl flex-col gap-8 px-5 py-12">
      <header className="flex flex-col gap-2">
        <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
      </header>

      <Pasaporte />
    </main>
  );
}
