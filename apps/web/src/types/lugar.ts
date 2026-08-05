/** Contrato de /api/v1/lugares. Debe reflejar los DTOs del backend. */

export type EstadoLugar = "BORRADOR" | "PUBLICADO" | "ARCHIVADO";
export type OrdenLugares = "ALFABETICO" | "MEJOR_VALORADOS" | "MAS_VISITADOS";

export interface Categoria {
  id: string;
  codigo: string;
  nombre: string;
  icono: string;
  colorHex: string;
}

export interface Horario {
  /** 0 = domingo ... 6 = sabado */
  diaSemana: number;
  horaApertura: string | null;
  horaCierre: string | null;
  cerrado: boolean;
}

export interface LugarResumen {
  id: string;
  slug: string;
  nombre: string;
  descripcion: string | null;
  categoria: Categoria;
  longitud: number;
  latitud: number;
  precioEntradaPen: string | null;
  duracionVisitaMin: number | null;
  estado: EstadoLugar;
  horarios: Horario[];
  calificacionPromedio: string;
  totalResenas: number;
  totalVisitas: number;
}

export interface LugarDetalle extends Omit<LugarResumen, "calificacionPromedio" | "totalResenas" | "totalVisitas"> {
  idioma: "ES" | "EN";
  traduccionPorDefecto: boolean;
  historia: string | null;
  consejos: string | null;
  distrito: { id: string; codigo: string; nombre: string; provincia: string };
  direccion: string | null;
  telefono: string | null;
  aceptaTarjeta: boolean | null;
  tieneBanos: boolean | null;
  accesibleSillaRuedas: boolean | null;
  aptoNinos: boolean | null;
  costoTaxiDesdePlazaPen: string | null;
  requiereGuia: boolean | null;
  abiertoAhora: boolean;
  fotos: { id: string; url: string }[];
  actualizadoEn: string;
}

/** Respuesta paginada de Spring Data. */
export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

/** Criterios del explorador; se reflejan tal cual en la URL. */
export interface CriteriosLugares {
  q?: string;
  categoriaId?: string;
  orden?: OrdenLugares;
  pagina?: number;
}
