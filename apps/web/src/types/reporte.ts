/** Contrato de /reportes. */

export type EstadoReporte =
  | "RECIBIDO"
  | "EN_REVISION"
  | "APROBADO"
  | "DESCARTADO"
  | "RESUELTO";

export interface TipoIncidente {
  id: string;
  codigo: string;
  nombre: string;
  icono: string;
  colorHex: string;
}

export interface Reporte {
  id: string;
  tipoCodigo: string;
  tipoNombre: string;
  tipoIcono: string;
  colorHex: string;
  descripcion: string;
  longitud: number;
  latitud: number;
  direccionReferencial: string | null;
  estado: EstadoReporte;
  esAnonimo: boolean;
  fotos: string[];
  /** Solo llega en la bandeja de moderacion; null en las respuestas publicas. */
  notasAdmin: string | null;
  reportadoEn: string;
}

export interface NuevoReporte {
  tipoIncidenteId: string;
  descripcion: string;
  longitud: number;
  latitud: number;
  direccionReferencial?: string;
  esAnonimo: boolean;
  nombreReportante?: string;
}
