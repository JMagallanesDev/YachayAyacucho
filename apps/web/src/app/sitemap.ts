import type { MetadataRoute } from "next";

import { proximosEventos } from "@/lib/eventos";
import { env } from "@/lib/env";
import { slugsPublicados } from "@/lib/lugares";
import { directorio } from "@/lib/negocios";
import { routing } from "@/i18n/routing";

/**
 * Mapa del sitio dinamico (Bloque 13).
 *
 * <p><strong>Cada URL se declara en los dos idiomas, no una vez.</strong> Un
 * sitemap con solo `/es/lugares/...` le dice a Google que la version inglesa no
 * existe, y la version inglesa deja de indexarse. Por eso cada entrada lleva
 * `alternates.languages` con sus equivalentes: es el hreflang recíproco, que es
 * lo que hace que un buscador entienda que son la misma pagina en dos lenguas y
 * no contenido duplicado.</p>
 *
 * <p>Es dinamico: pide al API los lugares publicados, los eventos y los
 * negocios aprobados. Si el API no responde, se emiten al menos las secciones
 * fijas — un sitemap incompleto es infinitamente mejor que un 500 que deja al
 * buscador sin ninguno.</p>
 *
 * <p>No entran las rutas privadas ni las de administracion: no tienen nada que
 * indexar y `robots.ts` ademas las prohibe.</p>
 */

/** Se regenera cada hora: el contenido patrimonial no cambia mas deprisa. */
export const revalidate = 3600;

type Entrada = MetadataRoute.Sitemap[number];

/** Construye una entrada con sus dos idiomas enlazados entre si. */
function entrada(
  ruta: string,
  opciones: { prioridad?: number; frecuencia?: Entrada["changeFrequency"]; fecha?: Date } = {},
): Entrada[] {
  const idiomas = Object.fromEntries(
    routing.locales.map((idioma) => [idioma, `${env.siteUrl}/${idioma}${ruta}`]),
  );

  return routing.locales.map((idioma) => ({
    url: `${env.siteUrl}/${idioma}${ruta}`,
    lastModified: opciones.fecha ?? new Date(),
    changeFrequency: opciones.frecuencia ?? "weekly",
    priority: opciones.prioridad ?? 0.5,
    alternates: { languages: idiomas },
  }));
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  // Las secciones fijas se emiten siempre, respondan o no las llamadas.
  const secciones: Entrada[] = [
    ...entrada("", { prioridad: 1.0, frecuencia: "daily" }),
    ...entrada("/lugares", { prioridad: 0.9, frecuencia: "weekly" }),
    ...entrada("/mapa", { prioridad: 0.8 }),
    ...entrada("/agenda", { prioridad: 0.8, frecuencia: "daily" }),
    ...entrada("/negocios", { prioridad: 0.7 }),
    ...entrada("/mapa-incidentes", { prioridad: 0.5 }),
    ...entrada("/reportar", { prioridad: 0.4 }),
  ];

  // Cada bloque va en su propio try: que falle la agenda no puede dejar fuera
  // del mapa a los quince lugares patrimoniales.
  let fichas: Entrada[] = [];
  try {
    const slugs = await slugsPublicados();
    fichas = slugs.flatMap((slug) =>
      entrada(`/lugares/${slug}`, { prioridad: 0.9, frecuencia: "monthly" }),
    );
  } catch {
    /* sin fichas en el mapa, pero con el resto */
  }

  let eventos: Entrada[] = [];
  try {
    // Solo los proximos: un evento pasado no aporta nada a un buscador y
    // ensuciaria el mapa con cientos de URLs muertas segun pasen los anios.
    const proximos = await proximosEventos("es", 20);
    eventos = proximos.flatMap((evento) =>
      entrada(`/agenda/${evento.id}`, { prioridad: 0.7, frecuencia: "daily" }),
    );
  } catch {
    /* sin eventos */
  }

  let negocios: Entrada[] = [];
  try {
    const pagina = await directorio("es", undefined, 0);
    negocios = pagina.content.flatMap((negocio) =>
      entrada(`/negocios/${negocio.id}`, { prioridad: 0.6, frecuencia: "monthly" }),
    );
  } catch {
    /* sin negocios */
  }

  return [...secciones, ...fichas, ...eventos, ...negocios];
}
