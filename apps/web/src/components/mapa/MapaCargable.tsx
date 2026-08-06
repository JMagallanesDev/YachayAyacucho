"use client";

import dynamic from "next/dynamic";

import type { ColeccionLugares, Ruta } from "@/types/mapa";

/**
 * Carga el mapa solo en el navegador.
 *
 * <p>MapLibre toca `window` y `WebGLRenderingContext` al importarse, asi que
 * si el modulo se evaluara durante el renderizado del servidor el build
 * fallaria. Con `ssr: false` el paquete —que no es pequeno— ni siquiera entra
 * en el HTML inicial: se descarga aparte, despues de que la pagina sea
 * utilizable.</p>
 *
 * <p>Este archivo existe porque `ssr: false` solo se admite dentro de un
 * Client Component; la pagina que lo usa sigue siendo Server Component.</p>
 *
 * <p>El respaldo mide <strong>exactamente</strong> lo que el mapa. Si midiera
 * distinto, al aparecer el mapa la pagina daria un salto y el CLS se saldria
 * del 0.1 que exige el RNF-24.</p>
 */
const MapaLugares = dynamic(
  () => import("@/components/mapa/MapaLugares").then((modulo) => modulo.MapaLugares),
  {
    ssr: false,
    loading: () => <EsqueletoMapa />,
  },
);

function EsqueletoMapa() {
  return (
    <div
      className="flex h-[70svh] animate-pulse items-center justify-center rounded-card bg-surface-muted"
      aria-busy="true"
      data-testid="esqueleto-mapa"
    />
  );
}

export function MapaCargable({
  lugares,
  rutas,
  claveMapTiler,
}: {
  lugares: ColeccionLugares;
  rutas: Ruta[];
  claveMapTiler: string;
}) {
  return <MapaLugares lugares={lugares} rutas={rutas} claveMapTiler={claveMapTiler} />;
}
