"use client";

import { useRouter } from "next/navigation";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { cerrarSesionEnServidor } from "@/lib/auth";
import { useSesion } from "@/stores/sesion";

export function PanelSesion() {
  const router = useRouter();
  const usuario = useSesion((estado) => estado.usuario);
  const cerrar = useSesion((estado) => estado.cerrar);
  const { comprobando } = useSesionRequerida();

  async function salir() {
    // Primero el servidor, para que revoque el refresh token en la base de
    // datos: borrar solo el estado local dejaria la sesion viva en el backend.
    await cerrarSesionEnServidor().catch(() => undefined);
    cerrar();
    router.push("/login");
    router.refresh();
  }

  if (comprobando || !usuario) {
    return <p className="text-text-muted">Recuperando tu sesion...</p>;
  }

  return (
    <section className="flex flex-col gap-6">
      <dl className="rounded-card border border-border-base bg-surface p-6">
        <div className="flex flex-col gap-1">
          <dt className="text-fluid-sm text-text-muted">Nombre</dt>
          <dd className="font-medium text-text">{usuario.nombre}</dd>
        </div>
        <div className="mt-4 flex flex-col gap-1">
          <dt className="text-fluid-sm text-text-muted">Correo</dt>
          <dd className="font-medium text-text">{usuario.email}</dd>
        </div>
        <div className="mt-4 flex flex-col gap-1">
          <dt className="text-fluid-sm text-text-muted">Rol</dt>
          <dd className="font-medium text-text">{usuario.rol}</dd>
        </div>
      </dl>

      <button
        type="button"
        onClick={salir}
        className="press min-h-touch self-start rounded-card border border-border-strong px-5 font-medium text-text"
      >
        Cerrar sesion
      </button>
    </section>
  );
}
