import { getTranslations, setRequestLocale } from "next-intl/server";

import { Link } from "@/i18n/navegacion";

import { FormularioNegocio } from "./FormularioNegocio";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "negocios" });
  return { title: t("registrarMiNegocio") };
}

/** Alta de un negocio en el directorio (RF-104). Exige sesion. */
export default async function PaginaRegistrarNegocio({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("negocios");

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-6 px-5 py-10">
      <Link
        href="/negocios"
        className="press w-fit text-fluid-sm font-medium text-text-muted underline-offset-4 hover:underline"
      >
        {t("volverAlDirectorio")}
      </Link>

      <header className="flex flex-col gap-3">
        <h1 className="text-fluid-2xl font-bold text-text">{t("registrarMiNegocio")}</h1>
        <p className="text-fluid-base text-text-muted">{t("registrarDescripcion")}</p>
      </header>

      <FormularioNegocio />
    </main>
  );
}
