import { getTranslations, setRequestLocale } from "next-intl/server";

import { BandejasUnificadas } from "./BandejasUnificadas";
import { GestionEventos } from "./GestionEventos";
import { GestionUsuarios } from "./GestionUsuarios";
import { PanelMetricas } from "./PanelMetricas";
import { RegistroActividad } from "./RegistroActividad";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "admin" });
  return { title: t("titulo") };
}

/**
 * Panel de administracion completo (Bloque 10).
 *
 * <p><strong>Que esta pagina se pinte no autoriza nada.</strong> El
 * {@code proxy.ts} solo comprueba que exista una cookie de sesion, que es lo
 * unico que puede saber sin llamar al API; y cada seccion de aqui hace su propia
 * peticion, que el backend responde con 403 a quien no tenga rol ADMIN. Un
 * usuario normal que fuerce esta URL vera el titulo y un aviso, nunca datos.</p>
 *
 * <p>El orden no es casual: primero lo que informa (metricas), despues lo que
 * exige accion (moderacion), luego la gestion, y al final la bitacora, que se
 * consulta cuando se busca algo concreto.</p>
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
    <main className="mx-auto flex min-h-svh w-full max-w-5xl flex-col gap-10 px-5 py-12">
      <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>

      <PanelMetricas />
      <BandejasUnificadas />
      <GestionEventos />
      <GestionUsuarios />
      <RegistroActividad />
    </main>
  );
}
