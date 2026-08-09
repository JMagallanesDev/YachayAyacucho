/** Contrato de /negocios (Bloque 11). */

export type EstadoNegocio = "PENDIENTE" | "APROBADO" | "RECHAZADO" | "SUSPENDIDO";

export interface CategoriaNegocio {
  id: string;
  codigo: string;
  nombre: string;
  icono: string;
}

export interface Negocio {
  id: string;
  nombre: string;
  descripcion: string | null;
  categoriaCodigo: string;
  categoriaNombre: string;
  categoriaIcono: string;
  distritoNombre: string;
  telefono: string | null;
  /** Ya normalizado por el backend: solo digitos, con prefijo de pais. */
  whatsapp: string | null;
  direccion: string | null;
  longitud: number | null;
  latitud: number | null;
  horarioTexto: string | null;
  estado: EstadoNegocio;
  registradoEn: string;
}

export interface DiaDeMetricas {
  fecha: string;
  visitas: number;
  clicsWhatsapp: number;
  clicsComoLlegar: number;
}

/** El negocio visto por su dueno: anade RUC, motivo y metricas agregadas. */
export interface MiNegocio {
  negocio: Negocio;
  ruc: string | null;
  motivoRechazo: string | null;
  metricas: DiaDeMetricas[];
  resumen: {
    visitas: number;
    clicsWhatsapp: number;
    clicsComoLlegar: number;
  };
}

export interface NuevoNegocio {
  nombre: string;
  categoriaId: string;
  distritoId: string;
  ruc?: string;
  telefono?: string;
  whatsapp?: string;
  direccion?: string;
  longitud?: number | null;
  latitud?: number | null;
  horarioTexto?: string;
  traducciones?: { idioma: "ES" | "EN"; descripcion: string }[];
}

/** Foto historica para el slider antes/despues (RF-11, RF-11b). */
export interface ImagenHistorica {
  id: string;
  titulo: string;
  urlHistorica: string;
  anioHistorico: number;
  /** Si falta, no hay comparacion que mostrar y el slider no se pinta. */
  urlActual: string | null;
  creditoHistorico: string | null;
  longitudCaptura: number | null;
  latitudCaptura: number | null;
  orden: number;
}
