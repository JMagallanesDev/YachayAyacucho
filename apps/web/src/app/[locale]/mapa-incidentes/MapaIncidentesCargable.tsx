"use client";

import dynamic from "next/dynamic";

import type { Reporte } from "@/types/reporte";

/**
 * Carga el mapa de incidentes solo en el navegador.
 *
 * <p>Mismo motivo que en el mapa de lugares del Bloque 5: MapLibre toca
 * `window` al importarse y el build fallaria si se evaluara en el servidor. El
 * respaldo mide lo mismo que el mapa para no provocar un salto de layout.</p>
 */
const MapaIncidentes = dynamic(
  () => import("./MapaIncidentes").then((modulo) => modulo.MapaIncidentes),
  {
    ssr: false,
    loading: () => (
      <div
        className="h-[60svh] animate-pulse rounded-card bg-surface-muted"
        aria-busy="true"
        data-testid="esqueleto-mapa-incidentes"
      />
    ),
  },
);

export function MapaIncidentesCargable({ incidentes }: { incidentes: Reporte[] }) {
  return <MapaIncidentes incidentes={incidentes} />;
}
