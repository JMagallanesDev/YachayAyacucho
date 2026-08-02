import { getTranslations, setRequestLocale } from "next-intl/server";
import Link from "next/link";
import { Suspense } from "react";

import { FormularioLogin } from "./FormularioLogin";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "login" });
  return { title: t("titulo") };
}

/**
 * Server Component: solo compone la pagina. Toda la interactividad vive en el
 * formulario, que es la hoja del arbol, siguiendo la regla del CLAUDE.md de
 * mantener 'use client' en las hojas.
 */
export default async function PaginaLogin({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("login");

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-md flex-col justify-center gap-8 px-5 py-12">
      <header className="flex flex-col gap-2">
        <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
      </header>

      {/* useSearchParams exige un limite de Suspense por encima. */}
      <Suspense>
        <FormularioLogin />
      </Suspense>

      <p className="text-fluid-sm text-text-muted">
        {t("sinCuenta")}{" "}
        <Link href={`/${locale}/registro`} className="font-medium text-primary underline">
          {t("crearCuenta")}
        </Link>
      </p>
    </main>
  );
}
