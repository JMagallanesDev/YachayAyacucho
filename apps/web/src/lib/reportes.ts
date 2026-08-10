import { pedirAutenticado } from "@/lib/auth";
import { env } from "@/lib/env";
import type { EstadoReporte, NuevoReporte, Reporte, TipoIncidente } from "@/types/reporte";
import { cabecerasPublicas } from "@/lib/cabeceras";

/**
 * Cliente de reportes ciudadanos.
 *
 * <p><strong>Crear un reporte NO usa `pedirAutenticado`.</strong> Es
 * deliberado: adjuntar el token cuando existe sesion enviaria al servidor una
 * identidad que el reporte anonimo no necesita. Aunque el backend la descarte,
 * el principio es no mandarla siquiera.</p>
 */

export async function tiposDeIncidente(idioma: string): Promise<TipoIncidente[]> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/reportes/tipos?idioma=${idioma.toUpperCase()}`, {
      headers: cabecerasPublicas(),
      next: { revalidate: 3600 },
    });
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    return [];
  }
}

/**
 * Envia una denuncia.
 *
 * <p>Va como multipart porque puede llevar fotos. Cuando el reporte es
 * <strong>anonimo no se manda ninguna cabecera de autorizacion</strong>, ni
 * siquiera si hay sesion abierta.</p>
 */
export async function crearReporte(
  datos: NuevoReporte,
  fotos: File[],
  idioma: string,
  token?: string | null,
): Promise<Reporte> {
  const cuerpo = new FormData();
  cuerpo.append(
    "reporte",
    new Blob([JSON.stringify(datos)], { type: "application/json" }),
  );
  for (const foto of fotos) {
    cuerpo.append("fotos", foto);
  }

  const cabeceras: Record<string, string> = { Accept: "application/json" };
  if (!datos.esAnonimo && token) {
    cabeceras.Authorization = `Bearer ${token}`;
  }

  const respuesta = await fetch(
    `${env.apiUrl}/reportes?idioma=${idioma.toUpperCase()}`,
    { method: "POST", body: cuerpo, headers: cabeceras },
  );

  if (!respuesta.ok) {
    const problema = await respuesta.json().catch(() => ({}));
    throw new Error(problema.detail ?? `El servidor respondio ${respuesta.status}`);
  }
  return respuesta.json();
}

/** Incidentes publicados dentro de un area. Publico, sin cuenta. */
export async function incidentesEnMapa(
  area: { oeste: number; sur: number; este: number; norte: number },
  idioma: string,
): Promise<Reporte[]> {
  try {
    const parametros = new URLSearchParams({
      oeste: String(area.oeste),
      sur: String(area.sur),
      este: String(area.este),
      norte: String(area.norte),
      idioma: idioma.toUpperCase(),
    });

    const respuesta = await fetch(`${env.apiUrl}/reportes/mapa?${parametros}`, {
      headers: cabecerasPublicas(),
      next: { revalidate: 60 },
    });
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    return [];
  }
}

export function misReportes(idioma: string) {
  return pedirAutenticado<Reporte[]>(`/reportes/mios?idioma=${idioma.toUpperCase()}`);
}

// ---- Moderacion (solo ADMIN) --------------------------------------------

export function bandejaReportes(idioma: string) {
  return pedirAutenticado<Reporte[]>(`/admin/reportes?idioma=${idioma.toUpperCase()}`);
}

export function cambiarEstadoReporte(
  reporteId: string,
  estado: EstadoReporte,
  notas?: string,
) {
  return pedirAutenticado<void>(`/admin/reportes/${reporteId}/estado`, {
    method: "POST",
    body: JSON.stringify({ estado, notas }),
  });
}
