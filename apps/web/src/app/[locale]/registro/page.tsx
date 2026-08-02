import { getTranslations, setRequestLocale } from "next-intl/server";
import Link from "next/link";

import { FormularioRegistro } from "./FormularioRegistro";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "registro" });
  return { title: t("titulo") };
}

export default async function PaginaRegistro({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("registro");

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-md flex-col justify-center gap-8 px-5 py-12">
      <header className="flex flex-col gap-2">
        <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
      </header>

      <FormularioRegistro />

      <p className="text-fluid-sm text-text-muted">
        {t("yaTienesCuenta")}{" "}
        <Link href={`/${locale}/login`} className="font-medium text-primary underline">
          {t("iniciarSesion")}
        </Link>
      </p>
    </main>
  );
}
