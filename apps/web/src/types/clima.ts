/** Contrato de /clima y /recomendaciones. */

import type { LugarResumen } from "@/types/lugar";

export interface Clima {
  temperatura: number | null;
  sensacion: number | null;
  humedad: number | null;
  viento: number | null;
  /** Codigo de OpenWeatherMap: Rain, Clear, Clouds... */
  condicion: string | null;
  icono: string | null;
  /** Claves i18n, no frases: el idioma lo decide el navegador. */
  consejos: string[];
  medidoEn: string | null;
  disponible: boolean;
  /** True cuando el proveedor no responde y esto es lo ultimo que se supo. */
  obsoleto: boolean;
}

export interface PronosticoDia {
  fecha: string;
  minima: number | null;
  maxima: number | null;
  condicion: string | null;
  icono: string | null;
  probabilidadLluvia: number | null;
  consejos: string[];
}

export interface Pronostico {
  dias: PronosticoDia[];
  medidoEn: string | null;
  disponible: boolean;
  obsoleto: boolean;
}

export interface Recomendacion {
  lugar: LugarResumen;
  puntuacion: number;
  motivos: string[];
}

export interface Planificacion {
  fecha: string;
  pronostico: PronosticoDia | null;
  pronosticoDisponible: boolean;
  sugerencias: Recomendacion[];
}
