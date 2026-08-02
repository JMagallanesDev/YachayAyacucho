import { timingSafeEqual } from "node:crypto";
import { revalidatePath } from "next/cache";
import { NextResponse, type NextRequest } from "next/server";

import { idiomas } from "@/i18n/routing";

/**
 * Revalidacion ISR bajo demanda (seccion 5.5 del plan).
 *
 * Lo llama el backend cuando un administrador guarda, edita o da de baja un
 * lugar. Con ISR las fichas se sirven como HTML pre-generado desde el CDN sin
 * tocar la base de datos; el precio es que una edicion no se ve hasta que la
 * pagina se regenera. Este endpoint cierra ese hueco: el admin guarda y la
 * pagina publica se actualiza en segundos, sin desplegar nada.
 */

/** El secreto viaja en cabecera: en la URL acabaria en los logs de acceso. */
const CABECERA_SECRETO = "X-Revalidate-Secret";

/**
 * Comparacion en tiempo constante.
 *
 * Con `===`, la comparacion se detiene en el primer byte que no coincide, y
 * ese tiempo es medible: probando byte a byte se puede reconstruir el secreto
 * sin conocerlo. `timingSafeEqual` siempre tarda lo mismo.
 *
 * Se comparan longitudes antes porque `timingSafeEqual` lanza si difieren; esa
 * comprobacion solo filtra la longitud del secreto, no su contenido.
 */
function secretoValido(recibido: string | null, esperado: string): boolean {
  if (!recibido) {
    return false;
  }
  const a = Buffer.from(recibido);
  const b = Buffer.from(esperado);
  return a.length === b.length && timingSafeEqual(a, b);
}

export async function POST(peticion: NextRequest) {
  const esperado = process.env.REVALIDATE_SECRET;

  if (!esperado) {
    // Sin secreto configurado el endpoint queda cerrado. Dejarlo abierto
    // permitiria a cualquiera forzar regeneraciones y tumbar el CDN.
    console.error("[revalidate] REVALIDATE_SECRET no esta configurada");
    return NextResponse.json({ error: "no configurado" }, { status: 503 });
  }

  if (!secretoValido(peticion.headers.get(CABECERA_SECRETO), esperado)) {
    // Sin detalle: quien no trae el secreto no merece pistas sobre por que.
    return NextResponse.json({ error: "no autorizado" }, { status: 401 });
  }

  let slug: string | undefined;
  try {
    ({ slug } = await peticion.json());
  } catch {
    return NextResponse.json({ error: "cuerpo invalido" }, { status: 400 });
  }

  if (!slug) {
    return NextResponse.json({ error: "falta el slug" }, { status: 400 });
  }

  // Se regeneran las dos versiones de idioma y los listados: una edicion
  // puede cambiar el nombre, que aparece tambien en el listado.
  const rutas = idiomas.flatMap((idioma) => [
    `/${idioma}/lugares/${slug}`,
    `/${idioma}/lugares`,
  ]);

  for (const ruta of rutas) {
    revalidatePath(ruta);
  }

  return NextResponse.json({ revalidado: true, rutas });
}
