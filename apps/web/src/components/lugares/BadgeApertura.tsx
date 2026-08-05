"use client";

import { useTranslations } from "next-intl";
import { useSyncExternalStore } from "react";

import { estaAbiertoAhora, proximaApertura } from "@/lib/geo";
import type { Horario } from "@/types/lugar";

/** Avisa cada minuto para que el badge no se quede colgado en "Abierto". */
function suscribirAlMinuto(alCambiar: () => void) {
  const temporizador = setInterval(alCambiar, 60_000);
  return () => clearInterval(temporizador);
}

/** Minuto actual. Estable dentro del mismo minuto, asi que no repinta de mas. */
function minutoActual() {
  return Math.floor(Date.now() / 60_000);
}

/** En el servidor no hay "ahora" valido: se devuelve null y no se pinta nada. */
function sinMinuto() {
  return null;
}

/**
 * Badge «abierto ahora / cerrado» (RF-09b).
 *
 * <p>Se calcula <strong>en el navegador</strong> a partir de los horarios, no
 * en el servidor. Estas paginas se sirven pre-generadas y cacheadas: un
 * "Abierto" calculado al generar el HTML seguiria diciendolo horas despues.
 * Los horarios, en cambio, no caducan.</p>
 *
 * <p>La hora se toma siempre en la zona de Ayacucho, asi que un visitante
 * desde Madrid ve el estado real del lugar y no el de su propio reloj.</p>
 *
 * <p>En el renderizado del servidor no pinta nada: alli no existe un "ahora"
 * util —el HTML queda cacheado— y emitir un estado que el cliente corrige al
 * instante provocaria un aviso de hidratacion y un parpadeo.</p>
 *
 * <p>{@code useSyncExternalStore} resuelve las dos cosas a la vez: distingue
 * servidor de cliente sin efectos, y con una suscripcion por minuto hace que
 * el badge cambie solo cuando el lugar abre o cierra mientras la pagina sigue
 * abierta.</p>
 */
export function BadgeApertura({ horarios }: { horarios: Horario[] }) {
  const t = useTranslations("lugares");
  const minuto = useSyncExternalStore(suscribirAlMinuto, minutoActual, sinMinuto);

  if (minuto === null || !horarios || horarios.length === 0) {
    return null;
  }

  const abierto = estaAbiertoAhora(horarios);
  const abre = abierto ? null : proximaApertura(horarios);

  return (
    <span
      data-testid="badge-apertura"
      data-abierto={abierto}
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-fluid-sm font-medium ${
        abierto ? "bg-success-subtle text-success" : "bg-surface-muted text-text-muted"
      }`}
    >
      <span
        className={`size-2 rounded-full ${abierto ? "bg-success" : "bg-text-muted"}`}
        aria-hidden="true"
      />
      {abierto ? t("abiertoAhora") : abre ? t("abreALas", { hora: abre }) : t("cerradoAhora")}
    </span>
  );
}
