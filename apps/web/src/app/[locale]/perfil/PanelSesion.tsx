"use client";

import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { cerrarSesionEnServidor } from "@/lib/auth";
import { useSesion } from "@/stores/sesion";

export function PanelSesion() {
  const t = useTranslations("perfil");
  const tc = useTranslations("comun");
  const idioma = useLocale();
  const router = useRouter();
  const usuario = useSesion((estado) => estado.usuario);
  const cerrar = useSesion((estado) => estado.cerrar);
  const { comprobando } = useSesionRequerida();

  async function salir() {
    // Primero el servidor, para que revoque el refresh token en la base de
    // datos: borrar solo el estado local dejaria la sesion viva en el backend.
    await cerrarSesionEnServidor().catch(() => undefined);
    cerrar();
    // Con el prefijo de idioma: sin el, el proxy redirigiria otra vez y el
    // usuario veria un salto extra.
    router.push(`/${idioma}/login`);
    router.refresh();
  }

  if (comprobando || !usuario) {
    return <p className="text-text-muted">{t("recuperando")}</p>;
  }

  return (
    <section className="flex flex-col gap-6">
      <dl className="rounded-card border border-border-base bg-surface p-6">
        <div className="flex flex-col gap-1">
          <dt className="text-fluid-sm text-text-muted">{t("nombre")}</dt>
          <dd className="font-medium text-text">{usuario.nombre}</dd>
        </div>
        <div className="mt-4 flex flex-col gap-1">
          <dt className="text-fluid-sm text-text-muted">{t("correo")}</dt>
          <dd className="font-medium text-text">{usuario.email}</dd>
        </div>
        <div className="mt-4 flex flex-col gap-1">
          <dt className="text-fluid-sm text-text-muted">{t("rol")}</dt>
          <dd className="font-medium text-text">{usuario.rol}</dd>
        </div>
      </dl>

      <button
        type="button"
        onClick={salir}
        className="press min-h-touch self-start rounded-card border border-border-strong px-5 font-medium text-text"
      >
        {tc("salir")}
      </button>
    </section>
  );
}
