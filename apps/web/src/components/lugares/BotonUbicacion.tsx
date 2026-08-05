"use client";

import { useTranslations } from "next-intl";
import { useEffect } from "react";

import { useUbicacion } from "@/stores/ubicacion";

/**
 * Pide la ubicacion para calcular distancias a pie (RF-09c).
 *
 * <p><strong>Nunca se pide al cargar la pagina.</strong> Un dialogo de
 * permiso que aparece antes de que el usuario entienda que gana con el se
 * deniega, y una denegacion es pegajosa: el navegador no vuelve a preguntar y
 * la funcionalidad queda muerta en ese dispositivo para siempre. Por eso hace
 * falta un gesto explicito.</p>
 *
 * <p>Al montar solo se <em>consulta</em> el estado del permiso, que no abre
 * ningun dialogo. De ahi salen tres comportamientos:</p>
 * <ul>
 *   <li><strong>Concedido:</strong> se usa en silencio, sin molestar.</li>
 *   <li><strong>Denegado:</strong> el boton no se muestra, porque pulsarlo no
 *       haria nada.</li>
 *   <li><strong>Sin decidir:</strong> se muestra el boton.</li>
 * </ul>
 */
export function BotonUbicacion() {
  const t = useTranslations("lugares");
  const permiso = useUbicacion((estado) => estado.permiso);
  const solicitar = useUbicacion((estado) => estado.solicitar);
  const comprobarPermiso = useUbicacion((estado) => estado.comprobarPermiso);

  useEffect(() => {
    void comprobarPermiso();
  }, [comprobarPermiso]);

  if (permiso === "concedido" || permiso === "denegado" || permiso === "desconocido") {
    return null;
  }

  return (
    <button
      type="button"
      onClick={solicitar}
      disabled={permiso === "pidiendo"}
      data-testid="boton-ubicacion"
      className="press min-h-touch self-start rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text disabled:opacity-60"
    >
      {permiso === "pidiendo" ? t("ubicandote") : t("verDistancia")}
    </button>
  );
}
