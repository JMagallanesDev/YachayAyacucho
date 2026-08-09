/** Contrato de /eventos (Bloque 9). */

import type { FechaISO } from "@/lib/fechas";

export type TipoEvento =
  | "RELIGIOSO"
  | "CIVICO"
  | "CULTURAL"
  | "GASTRONOMICO"
  | "ARTESANAL"
  | "MUSICAL"
  | "OTRO";

export type EstadoEvento = "BORRADOR" | "PUBLICADO" | "CANCELADO" | "ARCHIVADO";

/** Los cuatro estados del clima de un evento. Ninguno es un error (RF-88). */
export type EstadoClima = "PRONOSTICO" | "FUERA_DE_ALCANCE" | "NO_DISPONIBLE" | "PASADO";

export type Temporada = "LLUVIAS" | "SECA";

export interface PronosticoDia {
  fecha: FechaISO;
  minima: number | null;
  maxima: number | null;
  condicion: string | null;
  icono: string | null;
  probabilidadLluvia: number | null;
  consejos: string[];
}

export interface ClimaEvento {
  estado: EstadoClima;
  /** Solo viene con estado PRONOSTICO. */
  dia: PronosticoDia | null;
  temporada: Temporada | null;
  diasParaElEvento: number;
}

export interface Evento {
  id: string;
  idioma: string;
  traduccionSustituta: boolean;
  nombre: string;
  descripcion: string | null;
  organizador: string | null;
  tipo: TipoEvento;
  /** Dia de calendario `YYYY-MM-DD`: nunca convertir a Date sin `lib/fechas`. */
  fechaInicio: FechaISO;
  fechaFin: FechaISO;
  duracionDias: number;
  cloudinaryUrlPortada: string | null;
  /** Identificador de YouTube, nunca una URL: el embed lo compone el cliente (RF-12). */
  youtubeVideoId: string | null;
  lugarNombre: string | null;
  lugarSlug: string | null;
  distritoNombre: string;
  recurrenteAnual: boolean;
  estado: EstadoEvento;
}

export interface EventoDetalle {
  evento: Evento;
  clima: ClimaEvento;
  eventoOrigenId: string | null;
}

export interface DiaDeViaje {
  fecha: FechaISO;
  clima: ClimaEvento;
  eventoIds: string[];
}

export interface Visita {
  desde: FechaISO;
  hasta: FechaISO;
  dias: DiaDeViaje[];
  eventos: Evento[];
}

/** Alta y edicion desde el panel. */
export interface NuevoEvento {
  lugarId?: string | null;
  distritoId: string;
  tipo: TipoEvento;
  fechaInicio: FechaISO;
  fechaFin: FechaISO;
  cloudinaryUrlPortada?: string | null;
  recurrenteAnual: boolean;
  estado: EstadoEvento;
  traducciones: {
    idioma: "ES" | "EN";
    nombre: string;
    descripcion?: string;
    organizador?: string;
  }[];
}
