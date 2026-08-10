import { env } from "@/lib/env";

/**
 * Datos estructurados en JSON-LD (Bloque 13).
 *
 * <p>Es lo que convierte una ficha en un resultado enriquecido: sin esto, un
 * buscador ve un titulo y un parrafo; con esto sabe que la Catedral es una
 * atraccion turistica con coordenadas, horario y valoracion, y que la Semana
 * Santa es un evento con fechas y lugar.</p>
 *
 * <p><strong>Sobre el {@code <script>}.</strong> En el Bloque 12 React avisaba
 * al renderizar un script, pero ese era <em>ejecutable</em>. Un
 * {@code application/ld+json} es un bloque de datos que el navegador nunca
 * ejecuta, y es la forma que documentan tanto Google como Next para emitirlo.</p>
 *
 * <p>El contenido se serializa con {@code JSON.stringify}, de modo que nada de
 * lo que venga de la base de datos puede cerrar la etiqueta e inyectar codigo:
 * las comillas y las barras salen escapadas. Por si acaso se neutraliza tambien
 * la secuencia {@code </} , que es la unica que {@code JSON.stringify} deja
 * pasar y la que permitiria cerrar el script antes de tiempo.</p>
 */
export function DatosEstructurados({ datos }: { datos: Record<string, unknown> }) {
  const json = JSON.stringify(datos).replace(/</g, "\\u003c");

  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: json }}
    />
  );
}

/** Raiz comun de todos los grafos: la organizacion que publica. */
export function organizacion() {
  return {
    "@type": "Organization",
    name: env.appName,
    url: env.siteUrl,
  };
}

/** Migas de pan, para que el buscador muestre la jerarquia bajo el titulo. */
export function migas(idioma: string, tramos: { nombre: string; ruta: string }[]) {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: tramos.map((tramo, i) => ({
      "@type": "ListItem",
      position: i + 1,
      name: tramo.nombre,
      item: `${env.siteUrl}/${idioma}${tramo.ruta}`,
    })),
  };
}
