import { getTranslations, setRequestLocale } from "next-intl/server";

import { Link } from "@/i18n/navegacion";

import { MisNegocios } from "./MisNegocios";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "miNegocio" });
  return { title: t("titulo") };
}

/** Panel del dueno de negocio (RF-107). El `proxy.ts` ya exige sesion en /perfil. */
export default async function PaginaMiNegocio({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("miNegocio");

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-6 px-5 py-10">
      <Link
        href="/perfil"
        className="press w-fit text-fluid-sm font-medium text-text-muted underline-offset-4 hover:underline"
      >
        {t("volverAlPerfil")}
      </Link>

      <header className="flex flex-col gap-3">
        <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
      </header>

      <MisNegocios />
    </main>
  );
}
