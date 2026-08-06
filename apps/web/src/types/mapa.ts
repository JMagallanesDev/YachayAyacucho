/** Contrato de /lugares/mapa y /rutas. */

export interface PropiedadesLugar {
  id: string;
  slug: string;
  nombre: string;
  categoriaCodigo: string;
  categoriaNombre: string;
  color: string;
  icono: string;
}

export interface FeatureLugar {
  type: "Feature";
  geometry: { type: "Point"; coordinates: [number, number] };
  properties: PropiedadesLugar;
}

export interface ColeccionLugares {
  type: "FeatureCollection";
  features: FeatureLugar[];
}

export interface Parada {
  lugarId: string;
  slug: string;
  nombre: string;
  orden: number;
  longitud: number;
  latitud: number;
}

export interface Ruta {
  id: string;
  slug: string;
  nombre: string;
  descripcion: string | null;
  colorHex: string;
  icono: string;
  paradas: Parada[];
}
