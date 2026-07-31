import { env } from "@/lib/env";
import type { RespuestaSalud, ResultadoSalud } from "@/types/health";

/**
 * Cliente HTTP del backend.
 *
 * A partir del Bloque 3 este modulo crecera con el manejo de errores
 * ProblemDetail (RFC 7807) y la cabecera Authorization. Por ahora solo
 * necesita hablar con /health.
 */

const TIMEOUT_MS = 5000;

async function pedir<T>(ruta: string, init?: RequestInit): Promise<{ estado: number; cuerpo: T }> {
  const respuesta = await fetch(`${env.apiUrl}${ruta}`, {
    ...init,
    headers: { Accept: "application/json", ...init?.headers },
    signal: AbortSignal.timeout(TIMEOUT_MS),
  });

  return { estado: respuesta.status, cuerpo: (await respuesta.json()) as T };
}

/**
 * Consulta el estado del sistema.
 *
 * El backend responde 503 cuando algun componente esta caido, y ese
 * cuerpo tambien interesa: dice cual fallo. Por eso no se trata el 503
 * como un error, solo la imposibilidad de contactar al servidor.
 */
export async function obtenerSalud(): Promise<ResultadoSalud> {
  try {
    const { cuerpo } = await pedir<RespuestaSalud>("/health", {
      // El estado del sistema nunca se cachea: siempre se pregunta.
      cache: "no-store",
    });
    return { alcanzable: true, salud: cuerpo };
  } catch (error) {
    const motivo =
      error instanceof Error && error.name === "TimeoutError"
        ? `El backend no respondio en ${TIMEOUT_MS / 1000} s`
        : "No se pudo conectar con el backend";
    return { alcanzable: false, motivo };
  }
}
