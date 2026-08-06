"use client";

import "maplibre-gl/dist/maplibre-gl.css";

import MapaGL, { Layer, NavigationControl, Popup, Source } from "react-map-gl/maplibre";
import { useTranslations } from "next-intl";
import { useMemo, useState } from "react";

import { CENTRO_HUAMANGA, LIMITES_AYACUCHO, estiloMapTiler } from "@/lib/mapa";
import { env } from "@/lib/env";
import type { Reporte } from "@/types/reporte";

/**
 * Mapa publico de incidentes reportados (RF-74).
 *
 * <p>Se pinta en 2D y no en 3D, a diferencia del mapa de lugares: aqui lo que
 * importa es leer <em>donde</em> se concentran los danos, y la vista inclinada
 * dificulta comparar distancias entre puntos.</p>
 *
 * <p>Los incidentes van en una fuente GeoJSON coloreada por tipo, con el mismo
 * color que trae el catalogo, de modo que anadir un tipo nuevo no obliga a
 * tocar este componente.</p>
 */
export function MapaIncidentes({ incidentes }: { incidentes: Reporte[] }) {
  const t = useTranslations("incidentes");
  const [seleccionado, setSeleccionado] = useState<Reporte | null>(null);
  const [listo, setListo] = useState(false);

  const geojson = useMemo(
    () => ({
      type: "FeatureCollection" as const,
      features: incidentes.map((incidente) => ({
        type: "Feature" as const,
        geometry: {
          type: "Point" as const,
          coordinates: [incidente.longitud, incidente.latitud],
        },
        properties: {
          id: incidente.id,
          color: incidente.colorHex,
          tipo: incidente.tipoNombre,
        },
      })),
    }),
    [incidentes],
  );

  if (!env.maptilerKey) {
    return (
      <div
        data-testid="mapa-incidentes-sin-clave"
        className="flex h-[60svh] items-center justify-center rounded-card bg-surface-muted p-6 text-center text-fluid-sm text-text-muted"
      >
        {t("sinClave")}
      </div>
    );
  }

  return (
    <div
      className="relative h-[60svh] overflow-hidden rounded-card"
      data-testid="contenedor-mapa-incidentes"
      data-mapa-listo={listo}
    >
      <MapaGL
        initialViewState={{
          longitude: CENTRO_HUAMANGA.longitud,
          latitude: CENTRO_HUAMANGA.latitud,
          zoom: 13,
        }}
        mapStyle={estiloMapTiler(env.maptilerKey)}
        maxBounds={LIMITES_AYACUCHO}
        minZoom={7}
        maxZoom={18}
        interactiveLayerIds={["incidentes-punto"]}
        onLoad={() => setListo(true)}
        onClick={(evento) => {
          const punto = evento.features?.[0];
          if (!punto) return;
          const encontrado = incidentes.find((i) => i.id === punto.properties?.id);
          setSeleccionado(encontrado ?? null);
        }}
        style={{ width: "100%", height: "100%" }}
        attributionControl={{ compact: true }}
      >
        <NavigationControl position="top-right" />

        <Source id="incidentes" type="geojson" data={geojson}>
          <Layer
            id="incidentes-punto"
            type="circle"
            paint={{
              "circle-color": ["get", "color"],
              "circle-radius": 10,
              "circle-stroke-width": 2.5,
              "circle-stroke-color": "#ffffff",
              "circle-opacity": 0.9,
            }}
          />
        </Source>

        {seleccionado && (
          <Popup
            longitude={seleccionado.longitud}
            latitude={seleccionado.latitud}
            anchor="bottom"
            onClose={() => setSeleccionado(null)}
            maxWidth="280px"
          >
            <div className="flex flex-col gap-1.5 p-1" data-testid="popup-incidente">
              <span
                className="w-fit rounded-full px-2 py-0.5 text-xs font-medium"
                style={{
                  backgroundColor: `${seleccionado.colorHex}1a`,
                  color: seleccionado.colorHex,
                }}
              >
                {seleccionado.tipoNombre}
              </span>
              <p className="text-sm">{seleccionado.descripcion}</p>
              {/* Nunca se muestra quien reporto, ni siquiera si se identifico:
                  publicarlo lo expondria frente a quien denuncio. */}
              <span className="text-xs opacity-70">{t(`estado.${seleccionado.estado}`)}</span>
            </div>
          </Popup>
        )}
      </MapaGL>
    </div>
  );
}
