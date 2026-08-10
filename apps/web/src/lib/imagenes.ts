/**
 * Cargador de imagenes de Cloudinary para `next/image` (RNF-03).
 *
 * <p><strong>Por que un cargador propio y no el optimizador de Next.</strong>
 * Las fotos ya viven en Cloudinary, que redimensiona y convierte a WebP o AVIF
 * por su cuenta. Si se dejara el optimizador por defecto, cada foto viajaria de
 * Cloudinary al servidor de Next, se volveria a procesar alli y se serviria otra
 * vez: el mismo trabajo pagado dos veces, y en Vercel el optimizador se factura
 * por imagen. Con este cargador, Next genera el {@code srcset} y Cloudinary
 * sirve directamente cada tamano.</p>
 *
 * <p>Lo que se gana respecto a un {@code <img>} suelto no es el formato —eso ya
 * lo daba {@code f_auto}— sino las tres cosas que faltaban: {@code srcset} con
 * varios anchos, {@code loading="lazy"} por defecto, y sobre todo
 * {@code width}/{@code height} obligatorios, que es lo que de verdad evita que
 * la pagina salte cuando la foto termina de cargar (CLS).</p>
 */

interface ParametrosCargador {
  src: string;
  width: number;
  quality?: number;
}

/** Marca de una URL de entrega de Cloudinary. */
const SUBIDA = "/image/upload/";

export function cargadorCloudinary({ src, width, quality }: ParametrosCargador): string {
  // Una URL que no sea de Cloudinary se devuelve intacta: es mejor servir la
  // imagen sin optimizar que romper el enlace inventando una transformacion.
  if (!src.includes(SUBIDA)) {
    return src;
  }

  const transformacion = [
    "f_auto",
    `q_${quality ?? "auto"}`,
    `w_${width}`,
    // `c_limit` no recorta ni amplia: si la original es mas pequena que el
    // ancho pedido, se sirve tal cual en vez de estirarla y verse borrosa.
    "c_limit",
  ].join(",");

  const [base, resto] = src.split(SUBIDA);

  // Si la URL ya trae transformaciones (las que aplica el backend al subir),
  // se antepone la nuestra: Cloudinary encadena de izquierda a derecha y la
  // primera manda sobre el formato y el ancho.
  return `${base}${SUBIDA}${transformacion}/${resto}`;
}

/**
 * Tamanos que declara cada imagen segun donde se use.
 *
 * <p>El atributo {@code sizes} le dice al navegador que ancho ocupara la imagen
 * ANTES de haber calculado el diseno, y es lo que le permite elegir del
 * {@code srcset} sin descargar de mas. Sin el, el navegador supone el ancho de
 * la ventana completa y se baja siempre la version mas grande, que es peor que
 * no tener {@code srcset}.</p>
 */
export const TAMANOS = {
  /** Foto principal de una ficha: todo el ancho del contenido. */
  ficha: "(max-width: 768px) 100vw, 768px",
  /** Tarjeta dentro de una rejilla de dos columnas. */
  tarjeta: "(max-width: 640px) 100vw, 50vw",
  /** Miniatura pequena de una bandeja de moderacion. */
  miniatura: "112px",
} as const;
