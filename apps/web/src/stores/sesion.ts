import { create } from "zustand";

import type { Usuario } from "@/types/auth";

/**
 * Estado de sesion del cliente.
 *
 * NO usa el middleware `persist` de Zustand, y es la decision de seguridad
 * central del frontend: el access token vive **solo en memoria**. Guardarlo en
 * localStorage o sessionStorage lo dejaria al alcance de cualquier script que
 * llegue a ejecutarse en la pagina, que es exactamente lo que consigue un XSS.
 *
 * Al recargar la pagina se pierde, y eso es lo correcto: se recupera con un
 * refresh silencioso contra la cookie httpOnly, que el JavaScript no puede
 * leer ni robar.
 */
interface EstadoSesion {
  accessToken: string | null;
  usuario: Usuario | null;
  /** false hasta que termina el primer intento de refresh silencioso. */
  cargando: boolean;

  iniciar: (accessToken: string, usuario: Usuario) => void;
  cerrar: () => void;
  terminarCarga: () => void;
}

export const useSesion = create<EstadoSesion>((set) => ({
  accessToken: null,
  usuario: null,
  cargando: true,

  iniciar: (accessToken, usuario) => set({ accessToken, usuario, cargando: false }),
  cerrar: () => set({ accessToken: null, usuario: null, cargando: false }),
  terminarCarga: () => set({ cargando: false }),
}));

/** Lectura directa fuera de React, para el cliente HTTP. */
export const tokenActual = () => useSesion.getState().accessToken;
