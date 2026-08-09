/**
 * Fechas de viaje del visitante (RF-84b).
 *
 * <p><strong>Por que este archivo existe y no vive junto al selector.</strong>
 * La constante estaba exportada desde {@code SelectorFechasViaje.tsx}, que
 * lleva {@code "use client"}. Un Server Component puede importar de un modulo
 * de cliente, pero lo que recibe <em>no es el valor</em>: son referencias que
 * el bundler resuelve en el navegador. La pagina pedia la cookie con un nombre
 * que en el servidor no era la cadena esperada, asi que nunca encontraba nada y
 * el selector aparecia siempre con las fechas por defecto — aunque el navegador
 * la hubiera guardado bien.</p>
 *
 * <p>La regla que deja: <strong>lo que comparten servidor y cliente vive en un
 * modulo neutro</strong>, sin directiva.</p>
 */

import { aDiaUtc, aFechaISO, hoyEnAyacucho } from "@/lib/fechas";
import type { FechaISO } from "@/lib/fechas";

/** Nombre de la cookie donde viven las fechas del viaje. */
export const COOKIE_VIAJE = "fechas_viaje";

/** Tres meses: mas alla, el visitante volveria a elegirlas de todos modos. */
export const DIAS_DE_VIDA_COOKIE = 90;

/** Duracion propuesta cuando alguien entra por primera vez. */
const DIAS_POR_DEFECTO = 4;

export interface RangoDeViaje {
  desde: FechaISO;
  hasta: FechaISO;
}

/**
 * Lee la cookie {@code 2026-09-09..2026-09-12}, o propone los proximos dias.
 *
 * <p>El formato se valida antes de usarlo: una cookie la puede escribir
 * cualquiera y su contenido acaba en la URL del API. Ante cualquier duda se cae
 * a la propuesta por defecto, que nunca falla.</p>
 */
export function leerRangoDeViaje(cookie: string | undefined): RangoDeViaje {
  const hoy = hoyEnAyacucho();
  const porDefecto: RangoDeViaje = {
    desde: hoy,
    hasta: aFechaISO(new Date(aDiaUtc(hoy).getTime() + DIAS_POR_DEFECTO * 86_400_000)),
  };

  if (!cookie) {
    return porDefecto;
  }

  const partes = cookie.split("..");
  const formato = /^\d{4}-\d{2}-\d{2}$/;

  if (partes.length !== 2 || !formato.test(partes[0]) || !formato.test(partes[1])) {
    return porDefecto;
  }
  // Comparacion de cadenas: en formato ISO el orden alfabetico es el
  // cronologico, asi que no hace falta construir ninguna fecha.
  if (partes[1] < partes[0]) {
    return porDefecto;
  }

  return { desde: partes[0], hasta: partes[1] };
}
