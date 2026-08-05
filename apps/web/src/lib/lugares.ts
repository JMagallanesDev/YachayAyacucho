import { env } from "@/lib/env";
import type { CriteriosLugares, LugarDetalle, LugarResumen, Pagina } from "@/types/lugar";

/**
 * Cliente de /api/v1/lugares.
 *
 * <p>Sirve tanto al servidor (prefetch para ISR) como al navegador (TanStack
 * Query), porque el endpoint es publico y no necesita credenciales.</p>
 */

export const TAMANO_PAGINA = 12;

/** Clave de cache de TanStack Query. Los criterios la componen entera. */
export function claveLugares(criterios: CriteriosLugares, idioma: string) {
  return ["lugares", idioma, criterios] as const;
}

export function claveLugar(slug: string, idioma: string) {
  return ["lugar", slug, idioma] as const;
}

function construirUrl(criterios: CriteriosLugares, idioma: string): string {
  const parametros = new URLSearchParams({
    idioma: idioma.toUpperCase(),
    size: String(TAMANO_PAGINA),
    page: String(criterios.pagina ?? 0),
  });

  if (criterios.q?.trim()) {
    parametros.set("q", criterios.q.trim());
  }
  if (criterios.categoriaId) {
    parametros.set("categoriaId", criterios.categoriaId);
  }
  if (criterios.orden && criterios.orden !== "ALFABETICO") {
    parametros.set("orden", criterios.orden);
  }

  return `${env.apiUrl}/lugares?${parametros}`;
}

export async function buscarLugares(
  criterios: CriteriosLugares,
  idioma: string,
): Promise<Pagina<LugarResumen>> {
  const respuesta = await fetch(construirUrl(criterios, idioma), {
    headers: { Accept: "application/json" },
    // La lista se regenera cada 5 minutos en el servidor; en el navegador la
    // frescura la gobierna TanStack Query.
    next: { revalidate: 300 },
  });

  if (!respuesta.ok) {
    throw new Error(`El listado de lugares respondio ${respuesta.status}`);
  }
  return respuesta.json();
}

export async function obtenerLugar(slug: string, idioma: string): Promise<LugarDetalle | null> {
  const respuesta = await fetch(
    `${env.apiUrl}/lugares/${encodeURIComponent(slug)}?idioma=${idioma.toUpperCase()}`,
    {
      headers: { Accept: "application/json" },
      // ISR: la pagina se sirve pre-generada y el backend dispara su
      // regeneracion al guardar (webhook del Bloque 3). El plazo de 1 hora es
      // la red de seguridad por si ese aviso se pierde.
      next: { revalidate: 3600 },
    },
  );

  if (respuesta.status === 404) {
    return null;
  }
  if (!respuesta.ok) {
    throw new Error(`La ficha del lugar respondio ${respuesta.status}`);
  }
  return respuesta.json();
}

/**
 * Slugs publicados, para generar las fichas en compilacion.
 *
 * <p>Si el API no responde durante el build, se devuelve una lista vacia en
 * vez de romper el despliegue: las paginas se generaran bajo demanda en la
 * primera visita.</p>
 */
export async function slugsPublicados(): Promise<string[]> {
  try {
    const pagina = await fetch(`${env.apiUrl}/lugares?size=200`, {
      headers: { Accept: "application/json" },
    });
    if (!pagina.ok) {
      return [];
    }
    const datos: Pagina<LugarResumen> = await pagina.json();
    return datos.content.map((lugar) => lugar.slug);
  } catch {
    return [];
  }
}
