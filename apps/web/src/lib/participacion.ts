import { pedirAutenticado } from "@/lib/auth";
import type { Pagina } from "@/types/lugar";
import type { LugarResumen } from "@/types/lugar";
import type {
  CheckInResultado,
  MotivoReporte,
  Pasaporte,
} from "@/types/participacion";

/**
 * Cliente de favoritos, check-in, pasaporte y reportes.
 *
 * <p>Todo exige sesion, asi que todo pasa por `pedirAutenticado`, que renueva
 * el access token en silencio si caduco a mitad de la accion.</p>
 */

export function alternarFavorito(slug: string) {
  return pedirAutenticado<{ favorito: boolean }>(`/lugares/${slug}/favorito`, {
    method: "POST",
    body: JSON.stringify({}),
  }).then((r) => r.favorito);
}

/** Devuelve false en vez de lanzar: sin sesion, simplemente no es favorito. */
export function esFavorito(slug: string) {
  return pedirAutenticado<{ favorito: boolean }>(`/lugares/${slug}/favorito`)
    .then((r) => r.favorito)
    .catch(() => false);
}

export function misFavoritos(idioma: string) {
  return pedirAutenticado<Pagina<LugarResumen>>(
    `/perfil/favoritos?idioma=${idioma.toUpperCase()}&size=50`,
  );
}

/**
 * Registra una visita.
 *
 * <p>Se envia tambien la precision que reporta el navegador: el servidor
 * descarta lecturas demasiado imprecisas, que es el caso honesto mas frecuente
 * —alguien bajo techo cuyo navegador esta triangulando por IP—.</p>
 */
export function registrarCheckIn(
  slug: string,
  posicion: { longitud: number; latitud: number; precision?: number },
) {
  return pedirAutenticado<CheckInResultado>(`/lugares/${slug}/check-in`, {
    method: "POST",
    body: JSON.stringify(posicion),
  });
}

export function obtenerPasaporte(idioma: string) {
  return pedirAutenticado<Pasaporte>(`/perfil/pasaporte?idioma=${idioma.toUpperCase()}`);
}

export function reportarContenido(datos: {
  fotoId?: string;
  resenaId?: string;
  motivo: MotivoReporte;
}) {
  return pedirAutenticado<{ enRevision: boolean }>("/reportes-contenido", {
    method: "POST",
    body: JSON.stringify(datos),
  });
}
