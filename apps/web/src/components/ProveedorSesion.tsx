"use client";

import { useEffect } from "react";

import { renovarSesion } from "@/lib/auth";
import { useSesion } from "@/stores/sesion";

/**
 * Recupera la sesion al cargar la aplicacion.
 *
 * El access token vive en memoria, asi que una recarga de pagina lo borra. La
 * cookie httpOnly del refresh sobrevive, y con ella se pide un token nuevo sin
 * que el usuario tenga que volver a escribir sus credenciales.
 *
 * Si no hay cookie valida, simplemente se marca la carga como terminada: la
 * navegacion anonima esta permitida en todo el sitio (RF-34).
 */
export function ProveedorSesion({ children }: { children: React.ReactNode }) {
  const iniciar = useSesion((estado) => estado.iniciar);
  const terminarCarga = useSesion((estado) => estado.terminarCarga);

  useEffect(() => {
    let cancelado = false;

    renovarSesion()
      .then((sesion) => {
        if (!cancelado) {
          iniciar(sesion.accessToken, sesion.usuario);
        }
      })
      .catch(() => {
        if (!cancelado) {
          terminarCarga();
        }
      });

    return () => {
      cancelado = true;
    };
  }, [iniciar, terminarCarga]);

  return <>{children}</>;
}
