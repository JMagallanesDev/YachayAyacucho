import { env } from "@/lib/env";
import type { Clima, Planificacion, Pronostico, Recomendacion } from "@/types/clima";

/**
 * Cliente de /clima y /recomendaciones.
 *
 * <p>Todas las funciones degradan en vez de lanzar. El clima y las
 * recomendaciones acompanan a la pagina; que fallen no puede impedir que se
 * vea el patrimonio, que es el contenido de verdad.</p>
 */

const SIN_CLIMA: Clima = {
  temperatura: null,
  sensacion: null,
  humedad: null,
  viento: null,
  condicion: null,
  icono: null,
  consejos: [],
  medidoEn: null,
  disponible: false,
  obsoleto: false,
};

export function claveClima() {
  return ["clima"] as const;
}

export function claveRecomendaciones(idioma: string) {
  return ["recomendaciones", idioma] as const;
}

export async function obtenerClima(): Promise<Clima> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/clima`, {
      headers: { Accept: "application/json" },
      // Corto: el backend ya cachea 30 min en Redis, asi que aqui solo se
      // evita la rafaga de una misma sesion.
      next: { revalidate: 300 },
    });
    return respuesta.ok ? respuesta.json() : SIN_CLIMA;
  } catch {
    return SIN_CLIMA;
  }
}

export async function obtenerPronostico(): Promise<Pronostico> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/clima/pronostico`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 1800 },
    });
    return respuesta.ok
      ? respuesta.json()
      : { dias: [], medidoEn: null, disponible: false, obsoleto: false };
  } catch {
    return { dias: [], medidoEn: null, disponible: false, obsoleto: false };
  }
}

export async function obtenerRecomendaciones(idioma: string): Promise<Recomendacion[]> {
  try {
    const respuesta = await fetch(
      `${env.apiUrl}/recomendaciones?idioma=${idioma.toUpperCase()}`,
      {
        headers: { Accept: "application/json" },
        // Las recomendaciones dependen de la hora, asi que caducan rapido: a
        // las 17:00 deben empezar a proponer miradores.
        next: { revalidate: 600 },
      },
    );
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    return [];
  }
}

export async function planificar(fecha: string, idioma: string): Promise<Planificacion | null> {
  try {
    const respuesta = await fetch(
      `${env.apiUrl}/recomendaciones/planificador?fecha=${fecha}&idioma=${idioma.toUpperCase()}`,
      { headers: { Accept: "application/json" }, cache: "no-store" },
    );
    return respuesta.ok ? respuesta.json() : null;
  } catch {
    return null;
  }
}
