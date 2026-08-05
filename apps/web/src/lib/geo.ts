import type { Horario } from "@/types/lugar";

/**
 * Calculos geograficos y horarios que se hacen en el navegador.
 *
 * <p>Viven en el cliente por dos razones distintas:</p>
 * <ul>
 *   <li>La <strong>distancia</strong> depende de donde este el visitante, un
 *       dato que el servidor no tiene y que ademas cambiaria la respuesta
 *       para cada persona, haciendo inutil el cache del CDN.</li>
 *   <li>El <strong>estado abierto/cerrado</strong> depende de la hora actual,
 *       y estas paginas se sirven pre-generadas: un "Abierto" congelado a las
 *       9:00 seguiria diciendolo a medianoche.</li>
 * </ul>
 */

/** Huamanga esta a 2 761 msnm. */
export const ZONA_AYACUCHO = "America/Lima";

/**
 * Velocidad a pie ajustada por altitud (RF-09c).
 *
 * A nivel del mar se camina a unos 5 km/h. A los 2 760 m de Huamanga la
 * menor presion parcial de oxigeno reduce el rendimiento aerobico, y en
 * cuestas —que en Huamanga son constantes— la diferencia se nota mas. Se usa
 * 4 km/h como cifra conservadora.
 *
 * Este es el UNICO sitio donde vive el numero: cambiarlo aqui lo cambia en
 * toda la aplicacion.
 */
export const VELOCIDAD_CAMINANDO_KMH = 4;

const RADIO_TIERRA_KM = 6371;

/**
 * Distancia en kilometros entre dos puntos (formula de Haversine).
 *
 * <p>Trata la Tierra como una esfera. El error frente a un calculo
 * elipsoidal es de milesimas a escala urbana: para decir "a 12 minutos
 * caminando" sobra de largo, y evita traer una libreria entera.</p>
 */
export function distanciaKm(
  origen: { lat: number; lon: number },
  destino: { lat: number; lon: number },
): number {
  const aRadianes = (grados: number) => (grados * Math.PI) / 180;

  const dLat = aRadianes(destino.lat - origen.lat);
  const dLon = aRadianes(destino.lon - origen.lon);

  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(aRadianes(origen.lat)) * Math.cos(aRadianes(destino.lat)) * Math.sin(dLon / 2) ** 2;

  return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

/** Minutos a pie, redondeados y con un minimo de 1. */
export function minutosCaminando(km: number): number {
  return Math.max(1, Math.round((km / VELOCIDAD_CAMINANDO_KMH) * 60));
}

/**
 * A partir de aqui, ir andando deja de ser un plan.
 *
 * <p>Una hora y media a pie es el limite razonable dentro de Huamanga. Mas
 * alla —Vilcashuaman, Titankayocc— el dato util es la distancia, no los
 * minutos: "a 407 min caminando" es tecnicamente cierto y practicamente
 * inservible.</p>
 */
export const MINUTOS_MAXIMOS_A_PIE = 90;

/**
 * Como mostrar lo lejos que queda un lugar (RF-09c).
 *
 * <p>Devuelve minutos a pie si tiene sentido caminar, y kilometros si no.</p>
 */
export function proximidad(km: number): { tipo: "caminando"; minutos: number } | { tipo: "lejos"; km: number } {
  const minutos = minutosCaminando(km);
  return minutos <= MINUTOS_MAXIMOS_A_PIE
    ? { tipo: "caminando", minutos }
    : { tipo: "lejos", km: Math.round(km) };
}

/**
 * Hora y dia actuales en Ayacucho, sin importar donde este el visitante.
 *
 * <p>Se usa `Intl` con la zona fijada: el reloj del dispositivo puede estar
 * en Madrid o en Tokio, pero el lugar abre segun la hora de Ayacucho.</p>
 */
export function ahoraEnAyacucho(referencia: Date = new Date()): {
  diaSemana: number;
  minutosDelDia: number;
} {
  const formateador = new Intl.DateTimeFormat("en-US", {
    timeZone: ZONA_AYACUCHO,
    weekday: "short",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });

  const partes = Object.fromEntries(
    formateador.formatToParts(referencia).map((p) => [p.type, p.value]),
  );

  const dias: Record<string, number> = { Sun: 0, Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6 };
  // "24" aparece a medianoche en algunos entornos; equivale a 0.
  const hora = Number(partes.hour) % 24;

  return {
    diaSemana: dias[partes.weekday as string] ?? 0,
    minutosDelDia: hora * 60 + Number(partes.minute),
  };
}

function aMinutos(hora: string): number {
  const [h, m] = hora.split(":").map(Number);
  return h * 60 + m;
}

/**
 * Estado abierto/cerrado calculado al momento (RF-09b).
 *
 * <p>Incluye la hora de apertura y excluye la de cierre: a la hora exacta de
 * cierre el lugar ya no admite visitantes.</p>
 */
export function estaAbiertoAhora(horarios: Horario[], referencia?: Date): boolean {
  if (!horarios || horarios.length === 0) {
    return false;
  }

  const { diaSemana, minutosDelDia } = ahoraEnAyacucho(referencia);

  return horarios.some((h) => {
    if (h.diaSemana !== diaSemana || h.cerrado || !h.horaApertura || !h.horaCierre) {
      return false;
    }
    const desde = aMinutos(h.horaApertura);
    const hasta = aMinutos(h.horaCierre);
    return minutosDelDia >= desde && minutosDelDia < hasta;
  });
}

/** Proxima hora de apertura de hoy, para el mensaje "abre a las ...". */
export function proximaApertura(horarios: Horario[], referencia?: Date): string | null {
  const { diaSemana, minutosDelDia } = ahoraEnAyacucho(referencia);

  const siguientes = horarios
    .filter((h) => h.diaSemana === diaSemana && !h.cerrado && h.horaApertura)
    .map((h) => h.horaApertura as string)
    .filter((hora) => aMinutos(hora) > minutosDelDia)
    .sort();

  return siguientes[0] ?? null;
}
