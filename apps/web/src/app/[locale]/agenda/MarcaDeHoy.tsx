"use client";

import { useValorDelCliente } from "@/components/useValorDelCliente";
import { hoyEnAyacucho } from "@/lib/fechas";
import type { FechaISO } from "@/lib/fechas";

/**
 * Resalta la casilla de hoy en el calendario.
 *
 * <p>Es cliente por una razon concreta: cualquier HTML que se reutilice puede
 * traer un "hoy" viejo. La pagina se renderiza por peticion —lee searchParams—,
 * pero su HTML si pasa por la cache de navegacion del router y, en produccion,
 * por la del CDN. Un dia calculado en el servidor y guardado ahi acabaria
 * senalando la casilla equivocada. Calcularlo en el navegador no puede fallar.
 * Es el mismo criterio que la insignia de abierto/cerrado del Bloque 4.</p>
 *
 * <p>"Hoy" es el dia <strong>en Ayacucho</strong>, no en la zona de quien mira:
 * las fiestas ocurren alli, y para quien consulta desde Madrid de madrugada lo
 * relevante sigue siendo el dia de Huamanga.</p>
 */
export function MarcaDeHoy({ fecha }: { fecha: FechaISO }) {
  // En el servidor siempre `false`, de modo que el HTML y el primer render del
  // navegador son identicos; la marca aparece al terminar la hidratacion.
  const esHoy = useValorDelCliente(
    () => hoyEnAyacucho() === fecha,
    () => false,
  );

  if (!esHoy) {
    return null;
  }

  return (
    <span
      data-testid="marca-hoy"
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 rounded-card ring-2 ring-accent"
    />
  );
}
