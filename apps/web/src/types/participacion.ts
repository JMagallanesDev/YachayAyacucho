/** Contrato de favoritos, check-in, pasaporte y reportes. */

export interface CheckInResultado {
  id: string;
  lugarSlug: string;
  distanciaMetros: number;
  /** Lugares distintos visitados tras esta visita. */
  sellos: number;
  /** Codigos de las insignias recien ganadas, para celebrarlas. */
  insigniasNuevas: string[];
}

export interface Sello {
  lugarId: string;
  slug: string;
  nombre: string;
  categoria: string;
  colorCategoria: string;
  visitadoEn: string;
}

export interface InsigniaPasaporte {
  id: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  icono: string;
  obtenida: boolean;
  obtenidaEn: string | null;
}

export interface ProgresoRuta {
  rutaId: string;
  slug: string;
  nombre: string;
  colorHex: string;
  visitados: number;
  total: number;
  completada: boolean;
}

export interface Pasaporte {
  sellos: number;
  lugaresTotales: number;
  visitas: Sello[];
  insignias: InsigniaPasaporte[];
  rutas: ProgresoRuta[];
}

export type MotivoReporte =
  | "SPAM"
  | "OFENSIVO"
  | "FALSO"
  | "IRRELEVANTE"
  | "DERECHOS_AUTOR"
  | "OTRO";
