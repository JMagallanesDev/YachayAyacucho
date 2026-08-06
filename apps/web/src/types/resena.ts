/** Contrato de /lugares/{slug}/resenas y /lugares/{slug}/fotos. */

export type EstadoResena = "PUBLICADA" | "OCULTA" | "ELIMINADA" | "EN_REVISION";
export type EstadoFoto = "PENDIENTE" | "APROBADA" | "RECHAZADA" | "EN_REVISION";

export interface Resena {
  id: string;
  calificacion: number;
  comentario: string | null;
  autor: string;
  autorId: string;
  estado: EstadoResena;
  creadaEn: string;
  /** True si el autor la modifico despues de publicarla. */
  editada: boolean;
}

export interface Foto {
  id: string;
  url: string;
  miniatura: string;
  autor: string;
  estado: EstadoFoto;
  motivoRechazo: string | null;
  subidaEn: string;
}

/** Bandejas de moderacion (RF-49, RF-50). */
export interface FotoModeracion {
  id: string;
  url: string;
  autor: string;
  lugarSlug: string;
  estado: EstadoFoto;
  subidaEn: string;
}

export interface ResenaModeracion {
  id: string;
  calificacion: number;
  comentario: string | null;
  autor: string;
  lugarSlug: string;
  estado: EstadoResena;
  creadaEn: string;
  editada: boolean;
}
