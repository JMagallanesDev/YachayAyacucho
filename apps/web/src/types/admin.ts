/** Contrato del panel de administracion (Bloque 10). */

export interface PuntoDiario {
  fecha: string;
  valor: number;
}

export interface Reparto {
  etiqueta: string;
  color: string | null;
  valor: number;
}

export interface Totales {
  usuarios: number;
  lugares: number;
  eventos: number;
  resenas: number;
  fotos: number;
  reportes: number;
  checkIns: number;
  visitasTotales: number;
}

export interface Pendientes {
  fotos: number;
  resenas: number;
  reportes: number;
  negocios: number;
}

export interface Dashboard {
  totales: Totales;
  visitas: PuntoDiario[];
  registros: PuntoDiario[];
  lugaresPorCategoria: Reparto[];
  visitasPorSeccion: Reparto[];
  pendientes: Pendientes;
}

export type NombreRol = "VISITANTE" | "USUARIO" | "NEGOCIO" | "ADMIN";
export type EstadoUsuario = "ACTIVO" | "SUSPENDIDO" | "PENDIENTE";

/** Nunca trae contrasena ni hash: el DTO del backend no tiene ese campo. */
export interface UsuarioAdmin {
  id: string;
  email: string;
  nombre: string;
  rol: NombreRol;
  estado: EstadoUsuario;
  registradoEn: string;
  esTuCuenta: boolean;
}

export interface Actividad {
  id: string;
  accion: string;
  entidad: string;
  entidadId: string | null;
  detalles: string | null;
  autorNombre: string;
  autorEmail: string;
  ip: string | null;
  ocurridoEn: string;
}

/** Seccion del sitio sobre la que se agregan visitas. */
export type TipoPagina =
  | "HOME"
  | "LUGAR"
  | "MAPA"
  | "EVENTO"
  | "RUTA"
  | "DIRECTORIO"
  | "REPORTAR";
