/** Contrato de los endpoints /api/v1/auth del backend. */

export type NombreRol = "VISITANTE" | "USUARIO" | "NEGOCIO" | "ADMIN";

export interface Usuario {
  id: string;
  email: string;
  nombre: string;
  rol: NombreRol;
  registradoEn: string;
}

export interface RespuestaAutenticacion {
  /** JWT de acceso. Solo en memoria; nunca en localStorage. */
  accessToken: string;
  expiraEnSegundos: number;
  usuario: Usuario;
}

/** Error del API en formato ProblemDetail (RFC 7807). */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errorCode?: string;
  errores?: Record<string, string>;
}
