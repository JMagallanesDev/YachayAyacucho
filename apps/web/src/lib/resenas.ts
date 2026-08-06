import { env } from "@/lib/env";
import { pedirAutenticado } from "@/lib/auth";
import type { Foto, Resena } from "@/types/resena";
import type { Pagina } from "@/types/lugar";

/**
 * Cliente de resenas y fotos.
 *
 * <p>Las lecturas publicas usan `fetch` directo —no necesitan token— y las
 * escrituras pasan por `pedirAutenticado`, que renueva la sesion en silencio
 * si el access token caduco a media escritura.</p>
 */

export function claveResenas(slug: string) {
  return ["resenas", slug] as const;
}

export function claveFotos(slug: string) {
  return ["fotos", slug] as const;
}

const PAGINA_VACIA: Pagina<Resena> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 0,
  first: true,
  last: true,
};

/**
 * Reseñas de un lugar.
 *
 * <p>El modo importa. Desde el <strong>servidor</strong> se pide con
 * `revalidate: 60`, porque la ficha se pre-genera con ISR y un `no-store`
 * dentro de ella la volveria dinamica, perdiendo el cacheado de todo el
 * contenido patrimonial por culpa de las opiniones. Desde el
 * <strong>navegador</strong> se pide sin cache, para que quien acaba de
 * escribir vea su reseña inmediatamente.</p>
 */
export async function listarResenas(
  slug: string,
  modo: "servidor" | "navegador" = "servidor",
): Promise<Pagina<Resena>> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/lugares/${slug}/resenas?size=20`, {
      headers: { Accept: "application/json" },
      ...(modo === "servidor"
        ? { next: { revalidate: 60 } }
        : { cache: "no-store" as const }),
    });

    return respuesta.ok ? await respuesta.json() : PAGINA_VACIA;
  } catch {
    return PAGINA_VACIA;
  }
}

export async function listarFotos(slug: string): Promise<Foto[]> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/lugares/${slug}/fotos`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 300 },
    });
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    return [];
  }
}

export function crearResena(slug: string, datos: { calificacion: number; comentario?: string }) {
  return pedirAutenticado<Resena>(`/lugares/${slug}/resenas`, {
    method: "POST",
    body: JSON.stringify(datos),
  });
}

export function editarResena(
  slug: string,
  resenaId: string,
  datos: { calificacion: number; comentario?: string },
) {
  return pedirAutenticado<Resena>(`/lugares/${slug}/resenas/${resenaId}`, {
    method: "PUT",
    body: JSON.stringify(datos),
  });
}

export function borrarResena(slug: string, resenaId: string) {
  return pedirAutenticado<void>(`/lugares/${slug}/resenas/${resenaId}`, { method: "DELETE" });
}

/** La resena del usuario actual, o null si aun no ha dejado ninguna. */
export function miResena(slug: string) {
  return pedirAutenticado<Resena | undefined>(`/lugares/${slug}/resenas/mia`)
    .then((r) => r ?? null)
    .catch(() => null);
}

export function misFotos(slug: string) {
  return pedirAutenticado<Foto[]>(`/lugares/${slug}/fotos/mias`).catch(() => []);
}

/**
 * Sube una foto.
 *
 * <p>Va como `FormData` para que el navegador componga el multipart con su
 * `boundary`. La validacion de verdad —firma de bytes, decodificacion y
 * tamaño— ocurre en el servidor: aqui solo se filtra por comodidad, y nunca
 * se confia en ello.</p>
 */
export function subirFoto(slug: string, archivo: File) {
  const cuerpo = new FormData();
  cuerpo.append("archivo", archivo);

  return pedirAutenticado<Foto>(`/lugares/${slug}/fotos`, {
    method: "POST",
    body: cuerpo,
  });
}
