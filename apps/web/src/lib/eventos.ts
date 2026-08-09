import { pedirAutenticado } from "@/lib/auth";
import { env } from "@/lib/env";
import type { FechaISO } from "@/lib/fechas";
import type { Evento, EventoDetalle, NuevoEvento, Visita } from "@/types/evento";

/**
 * Cliente de la agenda cultural.
 *
 * <p>Las lecturas publicas no llevan token y se cachean con ISR. Las de
 * administracion pasan por `pedirAutenticado`, que adjunta el access token y
 * renueva la sesion si hiciera falta.</p>
 *
 * <p>Todas devuelven un valor vacio ante un fallo de red en vez de propagar la
 * excepcion: una agenda que no carga debe dejar la pagina en pie, no tumbarla.
 * La excepcion es `visita`, donde un rango invalido es informacion que el
 * usuario necesita ver.</p>
 */

export async function eventosDelMes(
  anio: number,
  mes: number,
  idioma: string,
  tipo?: string,
): Promise<Evento[]> {
  const parametros = new URLSearchParams({
    anio: String(anio),
    mes: String(mes),
    idioma: idioma.toUpperCase(),
  });
  if (tipo) {
    parametros.set("tipo", tipo);
  }

  try {
    const respuesta = await fetch(`${env.apiUrl}/eventos/calendario?${parametros}`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 300 },
    });
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    return [];
  }
}

/** Proximos eventos para la portada (RF-84). */
export async function proximosEventos(
  idioma: string,
  limite = 3,
  tipo?: string,
): Promise<Evento[]> {
  const parametros = new URLSearchParams({
    limite: String(limite),
    idioma: idioma.toUpperCase(),
  });
  if (tipo) {
    parametros.set("tipo", tipo);
  }

  try {
    const respuesta = await fetch(`${env.apiUrl}/eventos/proximos?${parametros}`, {
      headers: { Accept: "application/json" },
      // Cinco minutos: lo justo para que un evento que termina hoy salga de la
      // lista sin castigar al API con una peticion por visita.
      next: { revalidate: 300 },
    });
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    return [];
  }
}

export async function eventoPorId(id: string, idioma: string): Promise<EventoDetalle | null> {
  try {
    const respuesta = await fetch(
      `${env.apiUrl}/eventos/${id}?idioma=${idioma.toUpperCase()}`,
      {
        headers: { Accept: "application/json" },
        // Media hora: el contenido del evento casi no cambia, pero el clima
        // que viene dentro si, y a los eventos cercanos les conviene refrescarse.
        next: { revalidate: 1800 },
      },
    );
    return respuesta.ok ? respuesta.json() : null;
  } catch {
    return null;
  }
}

/**
 * Cruce de las fechas del viaje con la agenda (RF-84b).
 *
 * <p>Sin cache: depende de unas fechas que el visitante acaba de elegir, asi que
 * una respuesta guardada seria de otra persona.</p>
 */
export async function visita(
  desde: FechaISO,
  hasta: FechaISO,
  idioma: string,
): Promise<Visita | null> {
  const parametros = new URLSearchParams({ desde, hasta, idioma: idioma.toUpperCase() });

  try {
    const respuesta = await fetch(`${env.apiUrl}/eventos/durante-mi-visita?${parametros}`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });
    return respuesta.ok ? respuesta.json() : null;
  } catch {
    return null;
  }
}

// ---- Gestion (solo ADMIN) -----------------------------------------------

export function bandejaEventos(idioma: string) {
  return pedirAutenticado<Evento[]>(`/admin/eventos?idioma=${idioma.toUpperCase()}`);
}

export function crearEvento(datos: NuevoEvento, idioma: string) {
  return pedirAutenticado<EventoDetalle>(`/admin/eventos?idioma=${idioma.toUpperCase()}`, {
    method: "POST",
    body: JSON.stringify(datos),
  });
}

export function actualizarEvento(id: string, datos: NuevoEvento, idioma: string) {
  return pedirAutenticado<EventoDetalle>(`/admin/eventos/${id}?idioma=${idioma.toUpperCase()}`, {
    method: "PUT",
    body: JSON.stringify(datos),
  });
}

/** Clona una festividad recurrente a otro anio (RF-86). */
export function clonarEvento(
  id: string,
  anio: number,
  idioma: string,
  fechas?: { fechaInicio?: FechaISO; fechaFin?: FechaISO },
) {
  return pedirAutenticado<EventoDetalle>(
    `/admin/eventos/${id}/clonar?idioma=${idioma.toUpperCase()}`,
    { method: "POST", body: JSON.stringify({ anio, ...fechas }) },
  );
}

export function eliminarEvento(id: string) {
  return pedirAutenticado<void>(`/admin/eventos/${id}`, { method: "DELETE" });
}
