"use client";

/**
 * Lee un design token desde JavaScript (RF-89).
 *
 * <p><strong>Por que hace falta esto.</strong> Casi toda la interfaz consume
 * los tokens con utilidades de Tailwind y no necesita nada. Pero hay dos
 * consumidores que pintan fuera del DOM y solo entienden colores literales:
 * <strong>MapLibre</strong>, que dibuja sobre WebGL, y <strong>Chart.js</strong>,
 * que dibuja sobre un canvas. A esos hay que darles una cadena de color.</p>
 *
 * <p>La alternativa era escribir el hex a mano, y eso tenia dos consecuencias
 * que se veian: el mapa y los graficos <em>no seguian el modo oscuro</em> —se
 * quedaban en su paleta clara sobre fondo negro— y la paleta dejaba de tener una
 * sola fuente de verdad. Leyendo la variable del documento se arreglan las dos
 * cosas a la vez.</p>
 *
 * <p>Devuelve el valor <em>resuelto</em>: si {@code --nav-item} apunta a
 * {@code --color-piedra-600}, aqui llega ya el {@code oklch(...)} final, que es
 * lo que ambas librerias saben pintar.</p>
 */
export function token(nombre: string, respaldo = "#000000"): string {
  if (typeof window === "undefined") {
    // En el servidor no hay documento que consultar. Los dos consumidores son
    // componentes de cliente, asi que esta rama solo existe por seguridad.
    return respaldo;
  }
  const valor = getComputedStyle(document.documentElement).getPropertyValue(nombre).trim();
  return valor ? aColorDeLienzo(valor) : respaldo;
}

/**
 * Traduce un color CSS moderno a {@code #rrggbb}.
 *
 * <p><strong>Por que hace falta esta conversion a mano.</strong> La paleta se
 * declara en OKLCH, y Tailwind v4 registra los colores del tema con
 * {@code @property}, de modo que el navegador los <em>computa</em> y
 * {@code getComputedStyle} ya no devuelve el texto original sino el color
 * canonico: {@code lab(26.7596 1.73028 -30.3149)}.</p>
 *
 * <p>Eso es CSS impecable y ninguno de nuestros dos consumidores de lienzo lo
 * entiende. <strong>MapLibre</strong> usa el analizador de la especificacion de
 * estilos de Mapbox, muy anterior a los espacios de color amplios, y rechaza la
 * capa entera con «color expected»; el sintoma era un mapa sin chinchetas y un
 * aviso de React en cascada, porque el manejador de errores de MapLibre acababa
 * llamando a {@code setState} durante el render. <strong>Chart.js</strong> se
 * comporta igual.</p>
 *
 * <p>Se intento primero delegar en el navegador —asignar el color a un contexto
 * 2D y leerlo de vuelta—, que es la solucion habitual y no cuesta nada. No
 * sirve: el lienzo de Chromium tampoco acepta {@code lab()} todavia, asi que
 * devolvia el color intacto. De ahi que la conversion se haga aqui.</p>
 *
 * <p>Las formulas son las de la especificacion de CSS Color 4. {@code lab()} usa
 * blanco D50, de modo que la matriz a sRGB lleva ya la adaptacion de Bradford;
 * escribirla con el blanco D65 —el error facil— tine todos los colores.</p>
 */
export function aColorDeLienzo(color: string): string {
  const limpio = color.trim();

  // Lo que ya se entiende se deja tal cual: hex, rgb(), rgba() y los nombres.
  if (!limpio.startsWith("lab(") && !limpio.startsWith("oklch(") && !limpio.startsWith("oklab(")) {
    return limpio;
  }

  const numeros = limpio
    .slice(limpio.indexOf("(") + 1, limpio.lastIndexOf(")"))
    .replace(/\//g, " ")
    .split(/[\s,]+/)
    .filter(Boolean)
    .map((n) => parseFloat(n.replace("%", "")));

  if (numeros.length < 3 || numeros.some(Number.isNaN)) {
    return limpio;
  }

  const [uno, dos, tres] = numeros;
  const alfa = numeros.length > 3 ? numeros[3] : 1;

  let lineal: [number, number, number];

  if (limpio.startsWith("lab(")) {
    lineal = labALinealSrgb(uno, dos, tres);
  } else if (limpio.startsWith("oklch(")) {
    // OKLCH es OKLab en coordenadas polares: el croma y el angulo son el
    // modulo y el argumento del par (a, b).
    const angulo = (tres * Math.PI) / 180;
    lineal = oklabALinealSrgb(uno, dos * Math.cos(angulo), dos * Math.sin(angulo));
  } else {
    lineal = oklabALinealSrgb(uno, dos, tres);
  }

  const [r, g, b] = lineal.map(gamma).map((v) => Math.round(Math.min(1, Math.max(0, v)) * 255));

  const hex = `#${[r, g, b].map((v) => v.toString(16).padStart(2, "0")).join("")}`;
  return alfa < 1 ? `rgba(${r}, ${g}, ${b}, ${alfa})` : hex;
}

/** Lab con blanco D50 a sRGB lineal (CSS Color 4). */
function labALinealSrgb(L: number, a: number, b: number): [number, number, number] {
  const fy = (L + 16) / 116;
  const fx = fy + a / 500;
  const fz = fy - b / 200;

  const k = 24389 / 27;
  const e = 216 / 24389;

  const x = fx ** 3 > e ? fx ** 3 : (116 * fx - 16) / k;
  const y = L > k * e ? fy ** 3 : L / k;
  const z = fz ** 3 > e ? fz ** 3 : (116 * fz - 16) / k;

  // Blanco D50.
  const X = x * 0.3457 / 0.3585;
  const Y = y;
  const Z = z * (1 - 0.3457 - 0.3585) / 0.3585;

  return [
    3.1341359569958707 * X - 1.6173863321612538 * Y - 0.4906619460083532 * Z,
    -0.978795502912089 * X + 1.9161404272921342 * Y + 0.03344273116131949 * Z,
    0.07195537988411677 * X - 0.2289768264158322 * Y + 1.405386058324125 * Z,
  ];
}

/** OKLab a sRGB lineal (CSS Color 4). */
function oklabALinealSrgb(L: number, a: number, b: number): [number, number, number] {
  const l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3;
  const m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3;
  const s = (L - 0.0894841775 * a - 1.291485548 * b) ** 3;

  return [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  ];
}

/** Codificacion gamma de sRGB. */
function gamma(v: number): number {
  return v <= 0.0031308 ? 12.92 * v : 1.055 * Math.abs(v) ** (1 / 2.4) - 0.055;
}

/**
 * Los tokens que necesitan mapa y graficos, leidos de una vez.
 *
 * <p>Se agrupan en una funcion para que quien los use pueda volver a llamarla
 * al cambiar el tema y repintar con la paleta nueva.</p>
 */
export function tokensDeLienzo() {
  return {
    texto: token("--text"),
    textoTenue: token("--text-muted"),
    superficie: token("--surface"),
    borde: token("--border"),
    primario: token("--primary"),
    secundario: token("--secondary"),
    acento: token("--accent"),
    exito: token("--success"),
    /** Contorno de las chinchetas: siempre claro, van sobre la cartografia. */
    contorno: token("--sobre-foto-solido"),
    sobreFotoFg: token("--sobre-foto-fg"),
  };
}
