import { env } from "@/lib/env";
import { tokenActual, useSesion } from "@/stores/sesion";
import type { ProblemDetail, RespuestaAutenticacion, Usuario } from "@/types/auth";

/**
 * Cliente de autenticacion.
 *
 * Todas las llamadas van con `credentials: "include"` para que el navegador
 * adjunte la cookie httpOnly del refresh token, que es lo unico que sobrevive
 * a una recarga de la pagina.
 */

export class ErrorApi extends Error {
  constructor(
    readonly estado: number,
    readonly problema: ProblemDetail,
  ) {
    super(problema.detail ?? "Error inesperado");
  }
}

async function pedir<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const respuesta = await fetch(`${env.apiUrl}${ruta}`, {
    ...opciones,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...opciones.headers,
    },
  });

  if (!respuesta.ok) {
    const problema = (await respuesta.json().catch(() => ({}))) as ProblemDetail;
    throw new ErrorApi(respuesta.status, problema);
  }

  return respuesta.status === 204 ? (undefined as T) : ((await respuesta.json()) as T);
}

export function registrar(datos: { email: string; password: string; nombre: string }) {
  return pedir<Usuario>("/auth/register", {
    method: "POST",
    body: JSON.stringify(datos),
  });
}

export function iniciarSesion(datos: { email: string; password: string }) {
  return pedir<RespuestaAutenticacion>("/auth/login", {
    method: "POST",
    body: JSON.stringify(datos),
  });
}

/**
 * Renovacion en curso, compartida por todos los que la pidan a la vez.
 *
 * No es una optimizacion, es un requisito de correccion. El backend **rota**
 * el refresh token en cada uso e interpreta la reutilizacion de uno ya rotado
 * como un robo, revocando todas las sesiones del usuario. Si dos llamadas
 * salieran a la vez con la misma cookie, la segunda llegaria con un token ya
 * gastado y el sistema cerraria la sesion por su cuenta.
 *
 * Ocurre de verdad y en dos situaciones distintas:
 * - En desarrollo, React StrictMode ejecuta los efectos dos veces, asi que el
 *   proveedor de sesion pedia dos renovaciones seguidas.
 * - En produccion, varias peticiones que caduquen a la vez dispararian una
 *   renovacion cada una.
 *
 * Compartiendo una sola promesa, solo sale una peticion HTTP y todos reciben
 * su resultado.
 */
let renovacionEnCurso: Promise<RespuestaAutenticacion> | null = null;

/**
 * Renueva el access token con la cookie de refresh.
 *
 * La cabecera `X-Refresh-Request` es la defensa CSRF que acompana a
 * `SameSite=None` en produccion, donde frontend y backend estan en dominios
 * distintos: un formulario malicioso de otro sitio puede provocar la peticion,
 * pero no puede anadir cabeceras propias sin un preflight CORS que el backend
 * rechaza.
 */
export function renovarSesion(): Promise<RespuestaAutenticacion> {
  renovacionEnCurso ??= pedir<RespuestaAutenticacion>("/auth/refresh", {
    method: "POST",
    headers: { "X-Refresh-Request": "1" },
  }).finally(() => {
    renovacionEnCurso = null;
  });

  return renovacionEnCurso;
}

export function cerrarSesionEnServidor() {
  return pedir<void>("/auth/logout", { method: "POST" });
}

/**
 * Llamada autenticada con reintento silencioso.
 *
 * Si el access token caduco (401), pide uno nuevo y repite la peticion una
 * sola vez. Ese reintento es lo que permite que el token dure 15 minutos sin
 * que el usuario note nada. El limite de un intento evita bucles cuando el
 * refresh tambien ha caducado.
 */
export async function pedirAutenticado<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const conToken = (token: string | null): RequestInit => ({
    ...opciones,
    headers: {
      ...opciones.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  try {
    return await pedir<T>(ruta, conToken(tokenActual()));
  } catch (error) {
    if (!(error instanceof ErrorApi) || error.estado !== 401) {
      throw error;
    }

    try {
      const renovada = await renovarSesion();
      useSesion.getState().iniciar(renovada.accessToken, renovada.usuario);
      return await pedir<T>(ruta, conToken(renovada.accessToken));
    } catch {
      // El refresh tampoco vale: la sesion se acabo de verdad.
      useSesion.getState().cerrar();
      throw error;
    }
  }
}
