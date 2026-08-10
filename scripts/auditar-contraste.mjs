/**
 * Auditoria de contraste WCAG 2.1 AA sobre los design tokens (RNF-21).
 *
 * Se ejecuta con `pnpm a11y:contraste` y devuelve codigo de salida 1 si algun
 * par baja del minimo, de modo que sirve tambien en integracion continua.
 *
 * Lee tokens.css, resuelve cada token semantico hasta su OKLCH primitivo,
 * lo convierte a sRGB y calcula el contraste de cada par que existe de
 * verdad en la interfaz, en los DOS temas.
 */

import fs from "node:fs";

import path from "node:path";
import { fileURLToPath } from "node:url";

const RAIZ = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const CSS = fs.readFileSync(path.join(RAIZ, "apps/web/src/styles/tokens.css"), "utf8");

// ---------------------------------------------------------------------------
//  Conversion de color (la misma de lib/tokens.ts)
// ---------------------------------------------------------------------------

function oklabALinealSrgb(L, a, b) {
  const l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3;
  const m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3;
  const s = (L - 0.0894841775 * a - 1.291485548 * b) ** 3;
  return [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  ];
}

/** OKLCH -> canales sRGB lineales (sin gamma: es lo que pide la luminancia). */
function oklchALineal(L, C, H) {
  const ang = (H * Math.PI) / 180;
  return oklabALinealSrgb(L, C * Math.cos(ang), C * Math.sin(ang)).map((v) =>
    Math.min(1, Math.max(0, v)),
  );
}

/** Luminancia relativa de WCAG: se calcula sobre los canales LINEALES. */
function luminancia([r, g, b]) {
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contraste(colorA, colorB) {
  const a = luminancia(colorA);
  const b = luminancia(colorB);
  const claro = Math.max(a, b);
  const oscuro = Math.min(a, b);
  return (claro + 0.05) / (oscuro + 0.05);
}

// ---------------------------------------------------------------------------
//  Lectura de tokens
// ---------------------------------------------------------------------------

/** Todos los primitivos --color-x-N: oklch(...) */
const primitivos = new Map();
for (const m of CSS.matchAll(/(--color-[a-z]+-\d+):\s*oklch\(([^)]+)\)/g)) {
  const [L, C, H] = m[2].trim().split(/\s+/).map(Number);
  primitivos.set(m[1], { L, C, H });
}

/** Los oklch() escritos directamente en un semantico (p. ej. --surface). */
function comoOklch(valor) {
  const m = valor.match(/oklch\(([^)]+)\)/);
  if (!m) return null;
  const partes = m[1].trim().split(/[\s/]+/).map(Number);
  return { L: partes[0], C: partes[1], H: partes[2] };
}

/**
 * Extrae los semanticos de un bloque concreto.
 * Se recorre el archivo por bloques para separar el tema claro del oscuro.
 */
function semanticosDe(bloque) {
  const mapa = new Map();
  for (const m of bloque.matchAll(/(--[a-z-]+):\s*([^;]+);/g)) {
    const nombre = m[1];
    if (nombre.startsWith("--color-")) continue;
    mapa.set(nombre, m[2].trim());
  }
  return mapa;
}

/** Resuelve un semantico hasta su OKLCH final. */
function resolver(mapa, nombre, visitados = new Set()) {
  if (visitados.has(nombre)) return null;
  visitados.add(nombre);

  const valor = mapa.get(nombre);
  if (!valor) return null;

  const directo = comoOklch(valor);
  if (directo) return directo;

  const ref = valor.match(/var\((--[a-z0-9-]+)\)/);
  if (!ref) return null;

  if (primitivos.has(ref[1])) return primitivos.get(ref[1]);
  return resolver(mapa, ref[1], visitados);
}

// El bloque `:root {` inicial es el tema claro; `:root[data-theme="dark"]`, el oscuro.
const bloqueClaro = CSS.slice(CSS.indexOf(":root {"), CSS.indexOf("@media (prefers-color-scheme: dark)"));
const bloqueOscuro = CSS.slice(CSS.indexOf(':root[data-theme="dark"]'));

const temas = {
  claro: semanticosDe(bloqueClaro),
  oscuro: semanticosDe(bloqueOscuro),
};

// ---------------------------------------------------------------------------
//  Los pares que existen de verdad en la interfaz
// ---------------------------------------------------------------------------

const PARES = [
  ["--text", "--bg", "Texto principal sobre el fondo", 4.5],
  ["--text", "--surface", "Texto principal sobre tarjeta", 4.5],
  ["--text-muted", "--bg", "Texto tenue sobre el fondo", 4.5],
  ["--text-muted", "--surface", "Texto tenue sobre tarjeta", 4.5],
  ["--text-muted", "--surface-muted", "Texto tenue sobre superficie apagada", 4.5],
  ["--primary-fg", "--primary", "Texto del boton primario", 4.5],
  ["--secondary-fg", "--secondary", "Texto del boton secundario", 4.5],
  ["--accent-fg", "--accent", "Texto sobre el acento", 4.5],
  ["--primary", "--bg", "Enlace / acento carmin sobre el fondo", 4.5],
  ["--primary", "--surface", "Carmin sobre tarjeta", 4.5],
  ["--accent-text", "--surface", "Ocre COMO TEXTO sobre tarjeta", 4.5],
  ["--accent-text", "--accent-subtle", "Ocre como texto sobre su fondo suave", 4.5],
  ["--accent-text", "--bg", "Ocre como texto sobre el fondo", 4.5],
  ["--success", "--surface", "Verde de exito sobre tarjeta", 4.5],
  ["--danger", "--surface", "Rojo de error sobre tarjeta", 4.5],
  ["--nav-item", "--surface", "Destino inactivo de la barra", 4.5],
  ["--nav-item-activo", "--nav-indicador", "Destino activo sobre su indicador", 4.5],
  ["--text", "--esqueleto", "Texto sobre esqueleto", 3.0],
  ["--border-strong", "--bg", "Borde fuerte sobre el fondo", 3.0],
];

console.log("");
console.log("  CONTRASTE WCAG 2.1 AA DE LOS DESIGN TOKENS");
console.log("  ".padEnd(78, "="));
console.log(
  "  " +
    "Par".padEnd(42) +
    "claro".padStart(8) +
    "oscuro".padStart(9) +
    "minimo".padStart(9),
);
console.log("  ".padEnd(78, "-"));

const fallos = [];

for (const [frente, fondo, descripcion, minimo] of PARES) {
  const valores = {};
  for (const tema of ["claro", "oscuro"]) {
    const a = resolver(temas[tema], frente);
    const b = resolver(temas[tema], fondo);
    valores[tema] =
      a && b ? contraste(oklchALineal(a.L, a.C, a.H), oklchALineal(b.L, b.C, b.H)) : null;
  }

  const marca = (v) => (v === null ? "  ?  " : v >= minimo ? `${v.toFixed(2)} OK` : `${v.toFixed(2)} !!`);

  console.log(
    "  " +
      descripcion.padEnd(42) +
      marca(valores.claro).padStart(8) +
      marca(valores.oscuro).padStart(9) +
      String(minimo).padStart(9),
  );

  for (const tema of ["claro", "oscuro"]) {
    if (valores[tema] !== null && valores[tema] < minimo) {
      fallos.push({ descripcion, frente, fondo, tema, valor: valores[tema], minimo });
    }
  }
}

console.log("  ".padEnd(78, "="));
console.log(`  ${fallos.length} pares por debajo del minimo\n`);

for (const f of fallos) {
  console.log(
    `  !! ${f.descripcion} (${f.tema}): ${f.valor.toFixed(2)}:1, hace falta ${f.minimo}:1`,
  );
  console.log(`     ${f.frente} sobre ${f.fondo}`);
}

// Ayuda para corregir: que luminosidad OKLCH haria falta.
if (fallos.length) {
  console.log("\n  LUMINOSIDAD NECESARIA PARA CADA FALLO");
  console.log("  ".padEnd(78, "-"));
  for (const f of fallos) {
    const frente = resolver(temas[f.tema], f.frente);
    const fondo = resolver(temas[f.tema], f.fondo);
    const fondoLineal = oklchALineal(fondo.L, fondo.C, fondo.H);
    const fondoClaro = luminancia(fondoLineal) > 0.18;

    let mejor = null;
    for (let L = 0; L <= 1.0001; L += 0.002) {
      const c = contraste(oklchALineal(L, frente.C, frente.H), fondoLineal);
      if (c >= f.minimo) {
        // Sobre fondo claro se busca el mas CLARO que cumpla (menos cambio);
        // sobre fondo oscuro, el mas oscuro que cumpla.
        if (fondoClaro) mejor = L;
        else if (mejor === null) mejor = L;
      }
    }
    console.log(
      `  ${f.frente} (${f.tema}): L ${frente.L.toFixed(3)} -> ${mejor === null ? "imposible con ese croma" : mejor.toFixed(3)}`,
    );
  }
}

process.exit(fallos.length === 0 ? 0 : 1);
