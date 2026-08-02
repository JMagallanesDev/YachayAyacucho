"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { useSesion } from "@/stores/sesion";

/**
 * Guarda de cliente para las paginas privadas.
 *
 * Complementa al `proxy.ts`, que por si solo no basta. El proxy comprueba si
 * existe la cookie de refresh, y eso solo funciona cuando frontend y backend
 * comparten dominio: en el despliegue previsto (Vercel + Railway) la cookie
 * pertenece al dominio del backend y el proxy de Vercel **nunca podra verla**.
 *
 * Esta guarda cubre ese hueco: espera a que termine el intento de refresh
 * silencioso y, si no hay sesion, manda a /login. Sigue siendo experiencia de
 * usuario, no seguridad —la autorizacion real la decide el backend con
 * `@PreAuthorize`—, pero evita mostrar pantallas privadas vacias a quien no ha
 * iniciado sesion.
 *
 * @returns si todavia se esta comprobando la sesion
 */
export function useSesionRequerida(): { comprobando: boolean } {
  const router = useRouter();
  const cargando = useSesion((estado) => estado.cargando);
  const usuario = useSesion((estado) => estado.usuario);

  useEffect(() => {
    if (!cargando && !usuario) {
      router.replace(`/login?continuar=${encodeURIComponent(window.location.pathname)}`);
    }
  }, [cargando, usuario, router]);

  return { comprobando: cargando || !usuario };
}
