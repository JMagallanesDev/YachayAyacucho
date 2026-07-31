/**
 * Contrato del endpoint GET /api/v1/health del backend.
 * Debe reflejar HealthResponse.java.
 */

export type EstadoSalud = "UP" | "DOWN";

export interface ComponenteSalud {
  name: string;
  status: EstadoSalud;
  responseTimeMs: number;
  detail: string;
}

export interface RespuestaSalud {
  status: EstadoSalud;
  application: string;
  timestamp: string;
  components: ComponenteSalud[];
}

/**
 * Resultado que consume la interfaz: o bien la respuesta del backend,
 * o bien el hecho de que el backend no contesto.
 */
export type ResultadoSalud =
  | { alcanzable: true; salud: RespuestaSalud }
  | { alcanzable: false; motivo: string };
