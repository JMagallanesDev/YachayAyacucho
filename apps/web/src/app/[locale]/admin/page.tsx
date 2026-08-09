import { getTranslations, setRequestLocale } from "next-intl/server";

import { GestionEventos } from "./GestionEventos";
import { Moderacion } from "./Moderacion";
import { ModeracionReportes } from "./ModeracionReportes";
import { ResumenAdmin } from "./ResumenAdmin";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "admin" });
  return { title: t("titulo") };
}

/**
 * Panel de administracion. El dashboard completo llega en el Bloque 10.
 *
 * El `proxy.ts` impide entrar sin cookie de sesion, pero eso solo evita
 * mostrar el cascaron: quien llegue aqui sin rol ADMIN vera el titulo y un
 * mensaje de acceso denegado, porque el backend responde 403 a la llamada.
 */
export default async function PaginaAdmin({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("admin");

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-4xl flex-col gap-8 px-5 py-12">
      <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
      <ResumenAdmin />
      <GestionEventos />
      <Moderacion />
      <ModeracionReportes />
    </main>
  );
}
