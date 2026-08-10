import { env } from "@/lib/env";
import type { ColeccionLugares, Ruta } from "@/types/mapa";
import { cabecerasPublicas } from "@/lib/cabeceras";

/**
 * Datos y constantes del mapa (RF-17, RF-20, RF-22b).
 */

/**
 * Limites de la region Ayacucho, en el orden que espera MapLibre:
 * [oeste, sur, este, norte].
 *
 * <p>Los mismos numeros que valida el backend en
 * {@code DentroDeAyacuchoValidador}. Aqui sirven para que el mapa no se pueda
 * arrastrar fuera de la region (RF-22b): no es una restriccion de seguridad
 * —esa esta en el servidor— sino de orientacion, para que nadie acabe perdido
 * en el Pacifico sin entender que paso.</p>
 */
export const LIMITES_AYACUCHO: [number, number, number, number] = [
  -75.5, -15.5, -73.0, -12.5,
];

/** Plaza Mayor de Huamanga: el centro natural del mapa. */
export const CENTRO_HUAMANGA = { longitud: -74.2236, latitud: -13.1588 };

/**
 * Inclinacion de la vista 3D (RF-17).
 *
 * <p>Menos en movil: la pantalla es mas baja y con 55 grados el horizonte se
 * come casi toda la vista util.</p>
 */
export const PITCH_ESCRITORIO = 55;
export const PITCH_MOVIL = 45;

export function estiloMapTiler(clave: string): string {
  // El estilo "streets-v2" incluye la capa de edificios con su altura, que es
  // lo que alimenta la extrusion 3D.
  return `https://api.maptiler.com/maps/streets-v2/style.json?key=${clave}`;
}

const MAPA_VACIO: ColeccionLugares = { type: "FeatureCollection", features: [] };

/**
 * Puntos del mapa.
 *
 * <p>Degrada a una coleccion vacia en vez de lanzar. La pagina se pre-genera en
 * el build, y un despliegue no puede fallar porque el API tuviera un tropiezo
 * en ese instante: el mapa saldria sin chinchetas y se rellenaria solo en la
 * primera revalidacion, cinco minutos despues.</p>
 */
export async function lugaresDelMapa(idioma: string): Promise<ColeccionLugares> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/lugares/mapa?idioma=${idioma.toUpperCase()}`, {
      headers: cabecerasPublicas(),
      next: { revalidate: 300 },
    });
    return respuesta.ok ? respuesta.json() : MAPA_VACIO;
  } catch {
    return MAPA_VACIO;
  }
}

export async function rutasTematicas(idioma: string): Promise<Ruta[]> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/rutas?idioma=${idioma.toUpperCase()}`, {
      headers: cabecerasPublicas(),
      next: { revalidate: 3600 },
    });
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    // Sin rutas el mapa sigue siendo util: solo se queda sin polilineas.
    return [];
  }
}
