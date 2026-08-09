import { pedirAutenticado } from "@/lib/auth";
import { env } from "@/lib/env";
import type {
  CategoriaNegocio,
  ImagenHistorica,
  MiNegocio,
  Negocio,
  NuevoNegocio,
} from "@/types/negocio";

/** Cliente del directorio de negocios y de la historia visual. */

interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export async function directorio(
  idioma: string,
  categoriaId?: string,
  pagina = 0,
): Promise<Pagina<Negocio>> {
  const parametros = new URLSearchParams({
    idioma: idioma.toUpperCase(),
    page: String(pagina),
    size: "12",
  });
  if (categoriaId) {
    parametros.set("categoriaId", categoriaId);
  }

  try {
    const respuesta = await fetch(`${env.apiUrl}/negocios?${parametros}`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 300 },
    });
    return respuesta.ok
      ? respuesta.json()
      : { content: [], totalElements: 0, totalPages: 0, number: 0 };
  } catch {
    return { content: [], totalElements: 0, totalPages: 0, number: 0 };
  }
}

export async function categoriasDeNegocio(idioma: string): Promise<CategoriaNegocio[]> {
  try {
    const respuesta = await fetch(
      `${env.apiUrl}/negocios/categorias?idioma=${idioma.toUpperCase()}`,
      { headers: { Accept: "application/json" }, next: { revalidate: 3600 } },
    );
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    return [];
  }
}

/**
 * Ficha publica de un negocio.
 *
 * <p>Sin cache: abrir la ficha <strong>cuenta como visita</strong> en la
 * analitica, y una respuesta servida desde la cache de Next no llegaria al
 * backend y no se contaria. La ventana anti-recarga de 30 minutos ya evita que
 * recargar infle el numero.</p>
 */
export async function negocioPorId(id: string, idioma: string): Promise<Negocio | null> {
  try {
    const respuesta = await fetch(
      `${env.apiUrl}/negocios/${id}?idioma=${idioma.toUpperCase()}`,
      { headers: { Accept: "application/json" }, cache: "no-store" },
    );
    return respuesta.ok ? respuesta.json() : null;
  } catch {
    return null;
  }
}

// ---- Panel del dueno (RF-104, RF-107) -----------------------------------

export function misNegocios(idioma: string) {
  return pedirAutenticado<MiNegocio[]>(`/negocios/mios?idioma=${idioma.toUpperCase()}`);
}

export function miNegocio(id: string, idioma: string) {
  return pedirAutenticado<MiNegocio>(`/negocios/mios/${id}?idioma=${idioma.toUpperCase()}`);
}

export function registrarNegocio(datos: NuevoNegocio, idioma: string) {
  return pedirAutenticado<MiNegocio>(`/negocios?idioma=${idioma.toUpperCase()}`, {
    method: "POST",
    body: JSON.stringify(datos),
  });
}

export function actualizarNegocio(id: string, datos: NuevoNegocio, idioma: string) {
  return pedirAutenticado<MiNegocio>(`/negocios/mios/${id}?idioma=${idioma.toUpperCase()}`, {
    method: "PUT",
    body: JSON.stringify(datos),
  });
}

// ---- Moderacion (solo ADMIN) --------------------------------------------

export function bandejaNegocios(idioma: string) {
  return pedirAutenticado<Negocio[]>(`/admin/negocios?idioma=${idioma.toUpperCase()}`);
}

export function cambiarEstadoNegocio(id: string, estado: string, motivo?: string) {
  return pedirAutenticado<Negocio>(`/admin/negocios/${id}/estado`, {
    method: "POST",
    body: JSON.stringify({ estado, motivo }),
  });
}

// ---- Historia visual (RF-11, RF-11b) ------------------------------------

export async function historiaVisual(slug: string): Promise<ImagenHistorica[]> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/lugares/${slug}/historia-visual`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 3600 },
    });
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    return [];
  }
}

/** Puntos de captura para el modo «Parate aqui». */
export async function puntosDeCaptura(): Promise<ImagenHistorica[]> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/lugares/historia-visual/puntos`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 3600 },
    });
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    return [];
  }
}
