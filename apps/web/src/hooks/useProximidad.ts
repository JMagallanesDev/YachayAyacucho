"use client";

import { useEffect, useRef, useState } from "react";

import { distanciaKm } from "@/lib/geo";

/**
 * Modo «Estoy aqui»: avisa al llegar a un lugar (RF-19b).
 *
 * <p>Tres decisiones sostienen este hook, y las tres nacen de que el GPS del
 * mundo real no se parece al de un emulador:</p>
 *
 * <ol>
 *   <li><strong>Histeresis.</strong> Se entra a 50 m pero no se sale hasta los
 *       80. Con un unico umbral, el ruido normal de una lectura GPS —que oscila
 *       varios metros incluso quieto— haria que el aviso apareciera y
 *       desapareciera sin parar justo en el borde.</li>
 *   <li><strong>Supresion temporal.</strong> Una vez <em>descartado</em> el
 *       aviso de un lugar, no se repite en dos horas. Sin esto, alguien
 *       sentado en la Plaza Mayor recibiria el mismo aviso cada pocos
 *       segundos.
 *       <p>La marca se escribe al descartar o al alejarse, <strong>nunca al
 *       mostrar</strong>. Escribirla al mostrar parecia equivalente y no lo
 *       era: si el componente se vuelve a montar —StrictMode lo hace en
 *       desarrollo, y en produccion basta con navegar a la ficha y volver— el
 *       estado en memoria se pierde pero la marca sobrevive, y el aviso ya no
 *       reaparece aunque se siga delante del monumento. El sintoma era un
 *       «te avisaremos al llegar» eterno estando justo en el sitio.</p></li>
 *   <li><strong>Solo tras un gesto.</strong> El seguimiento no arranca por su
 *       cuenta: `watchPosition` mantiene el GPS activo y vacia la bateria, y
 *       ademas un permiso pedido sin contexto se deniega, con la particularidad
 *       de que una denegacion es permanente en ese dispositivo.</li>
 * </ol>
 */

const RADIO_ENTRADA_KM = 0.05;
const RADIO_SALIDA_KM = 0.08;
const SUPRESION_MS = 2 * 60 * 60 * 1000;
const CLAVE_SUPRESION = "yachay:proximidad-avisada";

export interface LugarCercano {
  slug: string;
  nombre: string;
  latitud: number;
  longitud: number;
}

type Avisados = Record<string, number>;

function leerAvisados(): Avisados {
  if (typeof window === "undefined") return {};
  try {
    // sessionStorage y no localStorage: la supresion vale para esta visita.
    // Si alguien vuelve manana, que le vuelva a avisar.
    return JSON.parse(window.sessionStorage.getItem(CLAVE_SUPRESION) ?? "{}");
  } catch {
    return {};
  }
}

function anotarAvisado(slug: string) {
  try {
    const avisados = leerAvisados();
    avisados[slug] = Date.now();
    window.sessionStorage.setItem(CLAVE_SUPRESION, JSON.stringify(avisados));
  } catch {
    // El modo privado de Safari puede lanzar al escribir. Sin supresion el
    // aviso se repetira, que es molesto pero no roto.
  }
}

export function useProximidad(lugares: LugarCercano[], activo: boolean) {
  const [cercano, setCercano] = useState<LugarCercano | null>(null);
  const dentroDe = useRef<string | null>(null);
  const observador = useRef<number | null>(null);

  useEffect(() => {
    if (!activo || typeof navigator === "undefined" || !navigator.geolocation) {
      return;
    }

    observador.current = navigator.geolocation.watchPosition(
      (posicion) => {
        const origen = {
          lat: posicion.coords.latitude,
          lon: posicion.coords.longitude,
        };

        const conDistancia = lugares
          .map((lugar) => ({
            lugar,
            km: distanciaKm(origen, { lat: lugar.latitud, lon: lugar.longitud }),
          }))
          .sort((a, b) => a.km - b.km);

        const masCercano = conDistancia[0];
        if (!masCercano) return;

        // Si ya se estaba dentro de un lugar, solo se sale al superar el radio
        // MAYOR. Esa diferencia es la histeresis.
        if (dentroDe.current === masCercano.lugar.slug) {
          if (masCercano.km > RADIO_SALIDA_KM) {
            // Al alejarse se cierra el aviso y se anota, para no repetirlo si
            // se vuelve a pasar por delante dentro de un rato.
            anotarAvisado(masCercano.lugar.slug);
            dentroDe.current = null;
            setCercano(null);
          }
          return;
        }

        if (masCercano.km <= RADIO_ENTRADA_KM) {
          const ultimoAviso = leerAvisados()[masCercano.lugar.slug] ?? 0;

          if (Date.now() - ultimoAviso > SUPRESION_MS) {
            dentroDe.current = masCercano.lugar.slug;
            setCercano(masCercano.lugar);
          }
        }
      },
      () => {
        // Permiso denegado o GPS sin senal: se deja de intentar en silencio.
        setCercano(null);
      },
      {
        enableHighAccuracy: true,
        // Una lectura de mas de un minuto no sirve para decir «estas aqui».
        maximumAge: 60_000,
        timeout: 15_000,
      },
    );

    return () => {
      // Imprescindible: sin este corte el GPS seguiria activo despues de salir
      // de la pagina y se comeria la bateria en segundo plano.
      if (observador.current !== null) {
        navigator.geolocation.clearWatch(observador.current);
        observador.current = null;
      }
    };
  }, [lugares, activo]);

  return {
    cercano,
    descartar: () => {
      // Descartar es la senal explicita de «ya lo he visto»: es aqui donde se
      // anota la supresion.
      if (cercano) {
        anotarAvisado(cercano.slug);
        dentroDe.current = null;
      }
      setCercano(null);
    },
  };
}
