/**
 * Fechas de calendario, sin desfase horario.
 *
 * <p><strong>El problema, que ya mordio una vez en el Bloque 4.</strong> El API
 * devuelve las fechas de los eventos como `"2027-03-21"`: un dia de calendario,
 * sin hora ni zona. Pero `new Date("2027-03-21")` NO crea ese dia — crea el
 * instante de la medianoche **en UTC**. Y al pedirle a ese instante que se
 * muestre en la zona del navegador, en Lima (UTC-5) sale el 20 de marzo.</p>
 *
 * <p>Es decir: la Semana Santa que empieza el 21 se anunciaria como el 20 a todo
 * el que estuviera al oeste de Greenwich, que es exactamente el publico de esta
 * aplicacion. Y en Tokio se veria bien, asi que el fallo pasaria inadvertido en
 * cualquier revision que no se hiciera desde America.</p>
 *
 * <p><strong>La regla del proyecto:</strong> ninguna fecha de evento se
 * convierte a `Date` sin pasar por aqui, y todo formateo fija
 * `timeZone: "UTC"`. Asi el dia civil que llego del servidor es el mismo que se
 * pinta, en cualquier huso horario del mundo.</p>
 */

/** Una fecha de calendario tal como viaja por el API: `YYYY-MM-DD`. */
export type FechaISO = string;

/**
 * Convierte `"2027-03-21"` en el instante de esa medianoche **en UTC**.
 *
 * <p>Emparejado siempre con `timeZone: "UTC"` al formatear, el resultado es que
 * el dia nunca se desplaza. El `Date` que devuelve es un vehiculo para las APIs
 * de `Intl`, no un momento real en el tiempo.</p>
 */
export function aDiaUtc(fecha: FechaISO): Date {
  const [anio, mes, dia] = fecha.split("-").map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia));
}

/** El camino inverso: de un `Date` en UTC a `"2027-03-21"`. */
export function aFechaISO(dia: Date): FechaISO {
  return dia.toISOString().slice(0, 10);
}

/** Construye una fecha ISO a partir de sus tres numeros, sin pasar por `Date`. */
export function fechaISO(anio: number, mes: number, dia: number): FechaISO {
  return `${anio}-${String(mes).padStart(2, "0")}-${String(dia).padStart(2, "0")}`;
}

/**
 * Formatea un dia de calendario en el idioma pedido, sin moverlo.
 *
 * <p>El `timeZone: "UTC"` de aqui es lo que impide el desfase, y por eso no es
 * configurable desde fuera.</p>
 */
export function formatearFecha(
  fecha: FechaISO,
  idioma: string,
  opciones: Intl.DateTimeFormatOptions = { day: "numeric", month: "long", year: "numeric" },
): string {
  return new Intl.DateTimeFormat(idioma, { ...opciones, timeZone: "UTC" }).format(aDiaUtc(fecha));
}

/**
 * Rango de fechas en una sola linea: "27 de marzo – 5 de abril de 2026".
 *
 * <p>Un evento de un solo dia no se escribe dos veces.</p>
 */
export function formatearRango(desde: FechaISO, hasta: FechaISO, idioma: string): string {
  if (desde === hasta) {
    return formatearFecha(desde, idioma);
  }

  const inicio = aDiaUtc(desde);
  const fin = aDiaUtc(hasta);
  const mismoAnio = inicio.getUTCFullYear() === fin.getUTCFullYear();

  const primero = formatearFecha(
    desde,
    idioma,
    mismoAnio ? { day: "numeric", month: "long" } : { day: "numeric", month: "long", year: "numeric" },
  );

  return `${primero} – ${formatearFecha(hasta, idioma)}`;
}

// ---------------------------------------------------------------------------
//  Que dia es hoy en Ayacucho
// ---------------------------------------------------------------------------

/** Ayacucho no cambia de hora, pero nombrar la zona lo hace explicito. */
const ZONA_AYACUCHO = "America/Lima";

/**
 * El dia de hoy **en Ayacucho**, no en la zona del visitante.
 *
 * <p>Importa para resaltar "hoy" en el calendario y para la cuenta regresiva:
 * quien consulta la agenda desde Madrid a la una de la madrugada sigue queriendo
 * saber que dia es en Huamanga, porque es alli donde ocurren las fiestas.</p>
 *
 * <p>Se obtiene con `Intl` en vez de restando horas: asi no hay que codificar
 * ningun desplazamiento, y si algun dia Peru adoptara horario de verano, esto
 * seguiria siendo correcto.</p>
 */
export function hoyEnAyacucho(): FechaISO {
  const partes = new Intl.DateTimeFormat("en-CA", {
    timeZone: ZONA_AYACUCHO,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());

  // "en-CA" formatea justo como YYYY-MM-DD, que es lo que se quiere.
  return partes;
}

/**
 * Dias entre hoy (en Ayacucho) y una fecha, para la cuenta regresiva.
 *
 * <p>Se resta sobre dias UTC, no sobre instantes: asi el resultado es un numero
 * entero de dias de calendario y no depende de la hora a la que se pregunte.</p>
 */
export function diasHasta(fecha: FechaISO, desde: FechaISO = hoyEnAyacucho()): number {
  const MS_POR_DIA = 86_400_000;
  return Math.round((aDiaUtc(fecha).getTime() - aDiaUtc(desde).getTime()) / MS_POR_DIA);
}

// ---------------------------------------------------------------------------
//  Rejilla del calendario mensual
// ---------------------------------------------------------------------------

/** Una casilla de la rejilla: su fecha y si pertenece al mes que se muestra. */
export interface CasillaCalendario {
  fecha: FechaISO;
  dia: number;
  delMes: boolean;
}

/**
 * Construye la rejilla de un mes, **empezando en lunes**.
 *
 * <p>Se rellenan los huecos con los dias vecinos, atenuados, para que la rejilla
 * sea siempre rectangular: un calendario al que le faltan casillas se lee mucho
 * peor que uno que muestra el final del mes anterior.</p>
 *
 * <p>Toda la aritmetica va sobre `Date.UTC`, de modo que ningun cambio de huso
 * puede correr la rejilla un dia.</p>
 */
export function rejillaDelMes(anio: number, mes: number): CasillaCalendario[] {
  const primero = new Date(Date.UTC(anio, mes - 1, 1));

  // getUTCDay() da 0 para domingo; se rota para que el lunes sea 0.
  const desplazamiento = (primero.getUTCDay() + 6) % 7;

  const inicio = new Date(Date.UTC(anio, mes - 1, 1 - desplazamiento));
  const casillas: CasillaCalendario[] = [];

  // Seis semanas cubren cualquier mes: incluso uno de 31 dias que empiece en
  // domingo ocupa 6 filas. Un numero fijo evita que la rejilla salte de alto al
  // cambiar de mes, que es un salto de maquetacion muy visible en movil.
  for (let i = 0; i < 42; i++) {
    const dia = new Date(inicio.getTime() + i * 86_400_000);
    casillas.push({
      fecha: aFechaISO(dia),
      dia: dia.getUTCDate(),
      delMes: dia.getUTCMonth() === mes - 1,
    });
  }

  return casillas;
}

/** Si un dia cae dentro del tramo de un evento, extremos incluidos. */
export function ocurreEn(dia: FechaISO, desde: FechaISO, hasta: FechaISO): boolean {
  return dia >= desde && dia <= hasta;
}

/** Mes anterior y siguiente, para los enlaces de navegacion. */
export function mesVecino(anio: number, mes: number, salto: -1 | 1): { anio: number; mes: number } {
  const total = anio * 12 + (mes - 1) + salto;
  return { anio: Math.floor(total / 12), mes: (total % 12) + 1 };
}
