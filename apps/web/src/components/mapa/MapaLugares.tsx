"use client";

import "maplibre-gl/dist/maplibre-gl.css";

import type { MapLayerMouseEvent, MapRef } from "react-map-gl/maplibre";
// Se importa como MapaGL y no como Map: el nombre `Map` taparia el Map nativo
// de JavaScript dentro de este modulo, y `new Map()` dejaria de compilar.
import MapaGL, {
  GeolocateControl,
  Layer,
  NavigationControl,
  Popup,
  ScaleControl,
  Source,
} from "react-map-gl/maplibre";
import { useLocale, useTranslations } from "next-intl";
import { useCallback, useMemo, useRef, useState } from "react";

import { AbrirEn } from "@/components/mapa/AbrirEn";
import { ControlesMapa } from "@/components/mapa/ControlesMapa";
import { Link } from "@/i18n/navegacion";
import {
  CENTRO_HUAMANGA,
  LIMITES_AYACUCHO,
  PITCH_ESCRITORIO,
  PITCH_MOVIL,
  estiloMapTiler,
} from "@/lib/mapa";
import type { ColeccionLugares, PropiedadesLugar, Ruta } from "@/types/mapa";

/**
 * Lo unico que se necesita de la geometria del evento.
 *
 * <p>Se declara aqui en vez de importar `@types/geojson`: ese paquete llega de
 * forma transitiva con MapLibre pero no es dependencia directa, y anadirlo al
 * proyecto entero por dos lineas no compensa.</p>
 */
type PuntoGeo = { coordinates: [number, number] };

/**
 * Capa de edificios extruidos del estilo «streets-v2» de MapTiler.
 *
 * <p>Su `minzoom` es 15, de ahi que el mapa arranque a ese nivel: con zoom 14
 * la vista se inclinaria sin que apareciera ningun edificio y el modo 3D
 * pareceria roto.</p>
 */
const CAPA_EDIFICIOS_3D = "Building 3D";

/**
 * Mapa de lugares patrimoniales (RF-17 a RF-22b).
 *
 * <p>Client Component obligatorio: MapLibre dibuja con WebGL, que no existe en
 * el servidor. La pagina que lo contiene sigue siendo Server Component y le
 * pasa los datos ya resueltos, de modo que el mapa no hace ninguna peticion
 * para pintarse.</p>
 *
 * <p><strong>Los puntos van dentro de una fuente GeoJSON, no como marcadores
 * de React.</strong> Es lo que permite cumplir el RNF-04 (mapa en menos de 2 s
 * y mas de 30 FPS con 200 marcadores): 200 elementos del DOM sincronizados con
 * la camara hunden los fotogramas, mientras que dentro de la fuente los dibuja
 * la GPU y el agrupamiento en clusters lo resuelve la propia biblioteca.</p>
 */
export function MapaLugares({
  lugares,
  rutas,
  claveMapTiler,
}: {
  lugares: ColeccionLugares;
  rutas: Ruta[];
  claveMapTiler: string;
}) {
  const t = useTranslations("mapa");
  const idioma = useLocale();
  const mapa = useRef<MapRef>(null);

  const [seleccionado, setSeleccionado] = useState<
    (PropiedadesLugar & { longitud: number; latitud: number }) | null
  >(null);
  const [tres_d, setTresD] = useState(true);
  /**
   * True cuando MapLibre ha cargado el estilo y pintado el primer fotograma.
   *
   * <p>Se expone en el DOM como `data-mapa-listo`. Es la senal fiable de que el
   * mapa funciona: el lienzo aparece en el DOM aunque no se dibuje nada, y las
   * peticiones de tiles salen desde un web worker, de modo que tampoco se ven
   * desde la pagina.</p>
   *
   * <p>Se usa el evento `load` y no `idle`: `idle` exige que no quede ningun
   * tile pendiente, algo que con renderizado por software puede no ocurrir
   * nunca. `load` significa exactamente lo que interesa saber —estilo aplicado
   * y mapa dibujando— y llega siempre.</p>
   */
  const [listo, setListo] = useState(false);

  /**
   * Ultimo error de MapLibre.
   *
   * <p>MapLibre no lanza excepciones cuando algo falla en el estilo o en una
   * fuente: emite un evento `error` y sigue adelante. Sin escuchar ese evento,
   * un mapa roto se ve exactamente igual que uno que aun esta cargando —un
   * rectangulo vacio— y no queda ni rastro en la consola.</p>
   */
  const [error, setError] = useState<string | null>(null);
  const [categoriasOcultas, setCategoriasOcultas] = useState<string[]>([]);
  const [rutaVisible, setRutaVisible] = useState<string | null>(null);

  const categorias = useMemo(() => {
    const vistas = new Map<string, { codigo: string; nombre: string; color: string }>();
    for (const punto of lugares.features) {
      const { categoriaCodigo, categoriaNombre, color } = punto.properties;
      if (!vistas.has(categoriaCodigo)) {
        vistas.set(categoriaCodigo, { codigo: categoriaCodigo, nombre: categoriaNombre, color });
      }
    }
    return [...vistas.values()].sort((a, b) => a.nombre.localeCompare(b.nombre));
  }, [lugares]);

  /**
   * Filtro de categorias (RF-22).
   *
   * <p>Se aplica con la propiedad `filter` de la capa, sin tocar los datos:
   * MapLibre reevalua el filtro en la GPU. Quitar y volver a poner la fuente
   * provocaria un parpadeo y perderia la animacion de los clusters.</p>
   */
  const filtroPuntos = useMemo(() => {
    // La condicion de "no es un grupo" debe conservarse SIEMPRE: si el filtro
    // de categorias la sustituyera, los circulos de cluster se pintarian
    // tambien como puntos individuales y se verian dos capas superpuestas.
    const noEsCluster = ["!", ["has", "point_count"]];

    if (categoriasOcultas.length === 0) {
      return noEsCluster as never;
    }
    return [
      "all",
      noEsCluster,
      ["!", ["in", ["get", "categoriaCodigo"], ["literal", categoriasOcultas]]],
    ] as never;
  }, [categoriasOcultas]);

  const alPulsar = useCallback((evento: MapLayerMouseEvent) => {
    const punto = evento.features?.[0];
    if (!punto) return;

    // Si es un cluster, se acerca en vez de abrir nada: pulsar un grupo de
    // doce lugares y ver la ficha de uno al azar seria desconcertante.
    if (punto.properties?.cluster) {
      const fuente = mapa.current?.getSource("lugares");
      const idCluster = punto.properties.cluster_id as number;
      // @ts-expect-error getClusterExpansionZoom existe en la fuente GeoJSON
      fuente?.getClusterExpansionZoom(idCluster, (error: unknown, zoom: number) => {
        if (error) return;
        const [longitud, latitud] = (punto.geometry as unknown as PuntoGeo).coordinates;
        mapa.current?.easeTo({ center: [longitud, latitud], zoom, duration: 500 });
      });
      return;
    }

    const [longitud, latitud] = (punto.geometry as unknown as PuntoGeo).coordinates;
    setSeleccionado({ ...(punto.properties as PropiedadesLugar), longitud, latitud });
  }, []);

  /**
   * Muestra u oculta los edificios extruidos del estilo.
   *
   * <p>No se anade una capa propia: el estilo «streets-v2» de MapTiler ya trae
   * una capa {@code Building 3D} de tipo `fill-extrusion` con las alturas
   * reales del edificio. Declarar otra encima obligaria a nombrar la fuente a
   * mano —se llama {@code maptiler_planet}, no {@code openmaptiles}— y
   * cualquier cambio de estilo la romperia en silencio.</p>
   *
   * <p>Se protege con try/catch porque la capa pertenece al estilo: si algun
   * dia se cambia por otro que no la tenga, el mapa debe seguir funcionando en
   * 2D en vez de reventar.</p>
   */
  const alternarEdificios = useCallback((visible: boolean) => {
    const instancia = mapa.current?.getMap();
    if (!instancia) return;
    try {
      if (instancia.getLayer(CAPA_EDIFICIOS_3D)) {
        instancia.setLayoutProperty(
          CAPA_EDIFICIOS_3D,
          "visibility",
          visible ? "visible" : "none",
        );
      }
    } catch {
      // Estilo sin edificios: la vista inclinada sigue funcionando igual.
    }
  }, []);

  const alternarTresD = useCallback(() => {
    const siguiente = !tres_d;
    setTresD(siguiente);
    alternarEdificios(siguiente);

    // Menos inclinacion en pantallas bajas: con 55 grados el horizonte se come
    // la vista util en un movil.
    const esMovil = typeof window !== "undefined" && window.innerWidth < 640;
    mapa.current?.easeTo({
      pitch: siguiente ? (esMovil ? PITCH_MOVIL : PITCH_ESCRITORIO) : 0,
      duration: 600,
    });
  }, [tres_d, alternarEdificios]);

  const rutaActiva = rutas.find((ruta) => ruta.slug === rutaVisible);

  const geoRuta = useMemo(() => {
    if (!rutaActiva) return null;
    return {
      type: "FeatureCollection" as const,
      features: [
        {
          type: "Feature" as const,
          properties: {},
          geometry: {
            type: "LineString" as const,
            // Ya vienen ordenadas por el backend. Reordenar aqui seria
            // duplicar una regla de negocio.
            coordinates: rutaActiva.paradas.map((p) => [p.longitud, p.latitud]),
          },
        },
      ],
    };
  }, [rutaActiva]);

  if (!claveMapTiler) {
    // Preferible a un rectangulo gris sin explicacion.
    return (
      <div
        data-testid="mapa-sin-clave"
        className="flex h-[70svh] items-center justify-center rounded-card bg-surface-muted p-6 text-center text-fluid-sm text-text-muted"
      >
        {t("sinClave")}
      </div>
    );
  }

  return (
    <div
      className="relative h-[70svh] overflow-hidden rounded-card"
      data-testid="contenedor-mapa"
      data-mapa-listo={listo}
      data-mapa-error={error ?? ""}
    >
      <MapaGL
        ref={mapa}
        initialViewState={{
          longitude: CENTRO_HUAMANGA.longitud,
          latitude: CENTRO_HUAMANGA.latitud,
          zoom: 15,
          pitch: PITCH_ESCRITORIO,
        }}
        mapStyle={estiloMapTiler(claveMapTiler)}
        // RF-22b: no se puede arrastrar fuera de Ayacucho.
        maxBounds={LIMITES_AYACUCHO}
        minZoom={7}
        maxZoom={18}
        interactiveLayerIds={["lugares-punto", "lugares-cluster"]}
        onClick={alPulsar}
        onLoad={() => setListo(true)}
        onError={(evento) => {
          const mensaje = evento.error?.message ?? "error desconocido";
          setError(mensaje);
          console.error("[mapa] MapLibre:", mensaje);
        }}
        // Sin esto, el gesto de arrastre sobre el mapa haria scroll de pagina
        // en movil y el mapa resultaria imposible de mover.
        style={{ width: "100%", height: "100%" }}
        attributionControl={{ compact: true }}
      >
        <NavigationControl position="top-right" visualizePitch />
        <ScaleControl position="bottom-left" />
        {/* RF-19. No dispara solo: exige que la persona lo pulse. */}
        <GeolocateControl
          position="top-right"
          trackUserLocation
          positionOptions={{ enableHighAccuracy: true }}
        />

        {/* ---- Ruta tematica (RF-20) ------------------------------------ */}
        {geoRuta && rutaActiva && (
          <Source id="ruta" type="geojson" data={geoRuta}>
            <Layer
              id="ruta-linea"
              type="line"
              layout={{ "line-join": "round", "line-cap": "round" }}
              paint={{
                "line-color": rutaActiva.colorHex,
                "line-width": 4,
                "line-opacity": 0.9,
              }}
            />
          </Source>
        )}

        {/* ---- Lugares con clusters (RF-18) ----------------------------- */}
        <Source
          id="lugares"
          type="geojson"
          data={lugares}
          cluster
          clusterRadius={50}
          clusterMaxZoom={15}
        >
          <Layer
            id="lugares-cluster"
            type="circle"
            filter={["has", "point_count"]}
            paint={{
              "circle-color": "#24406E",
              // El radio crece con el numero: un grupo de 30 debe verse mayor
              // que uno de 3 sin necesidad de leer la cifra.
              "circle-radius": ["step", ["get", "point_count"], 18, 10, 24, 30, 30],
              "circle-opacity": 0.9,
              "circle-stroke-width": 2,
              "circle-stroke-color": "#ffffff",
            }}
          />
          <Layer
            id="lugares-cluster-numero"
            type="symbol"
            filter={["has", "point_count"]}
            layout={{
              "text-field": ["get", "point_count_abbreviated"],
              "text-size": 13,
            }}
            paint={{ "text-color": "#ffffff" }}
          />
          <Layer
            id="lugares-punto"
            type="circle"
            filter={filtroPuntos}
            paint={{
              // Color por categoria, tomado del propio dato: asi anadir una
              // categoria en el backend no obliga a tocar el mapa.
              "circle-color": ["get", "color"],
              "circle-radius": 9,
              "circle-stroke-width": 2.5,
              "circle-stroke-color": "#ffffff",
            }}
          />
        </Source>

        {seleccionado && (
          <Popup
            longitude={seleccionado.longitud}
            latitude={seleccionado.latitud}
            anchor="bottom"
            onClose={() => setSeleccionado(null)}
            closeButton
            maxWidth="280px"
          >
            <div className="flex flex-col gap-2 p-1" data-testid="popup-lugar">
              <span
                className="w-fit rounded-full px-2 py-0.5 text-xs font-medium"
                style={{
                  backgroundColor: `${seleccionado.color}1a`,
                  color: seleccionado.color,
                }}
              >
                {seleccionado.categoriaNombre}
              </span>
              <strong className="text-sm">{seleccionado.nombre}</strong>
              <Link
                href={`/lugares/${seleccionado.slug}`}
                className="text-sm font-medium text-primary underline-offset-4 hover:underline"
              >
                {t("verFicha")}
              </Link>
              <AbrirEn
                longitud={seleccionado.longitud}
                latitud={seleccionado.latitud}
                nombre={seleccionado.nombre}
              />
            </div>
          </Popup>
        )}
      </MapaGL>

      <ControlesMapa
        categorias={categorias}
        categoriasOcultas={categoriasOcultas}
        alAlternarCategoria={(codigo) =>
          setCategoriasOcultas((previas) =>
            previas.includes(codigo)
              ? previas.filter((c) => c !== codigo)
              : [...previas, codigo],
          )
        }
        rutas={rutas}
        rutaVisible={rutaVisible}
        alElegirRuta={setRutaVisible}
        tresD={tres_d}
        alAlternarTresD={alternarTresD}
        idioma={idioma}
      />
    </div>
  );
}
