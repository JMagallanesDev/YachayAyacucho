/**
 * Lighthouse sobre las cinco paginas criticas del build de PRODUCCION.
 *
 * Se mide contra `next start` y no contra el servidor de desarrollo: en
 * desarrollo se mediria el coste del compilador, no el del producto.
 */

import { execSync, spawn } from "node:child_process";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import lighthouse from "lighthouse";

try {
  execSync("taskkill /IM msedge.exe /F", { stdio: "ignore" });
} catch {
  /* no habia ninguno */
}

const BASE = process.env.BASE ?? "http://localhost:3001";
import path from "node:path";
import { fileURLToPath } from "node:url";

const RAIZ = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";

function sql(consulta) {
  return execSync(
    `docker compose exec -T postgres psql -U postgres -d huamanga -tAc "${consulta}"`,
    { cwd: RAIZ, encoding: "utf-8" },
  ).trim();
}

const slug = sql("SELECT slug FROM lugar WHERE estado='PUBLICADO' ORDER BY slug LIMIT 1");

const PAGINAS = [
  { nombre: "Portada", ruta: "/es" },
  { nombre: "Listado de lugares", ruta: "/es/lugares" },
  { nombre: "Ficha de lugar", ruta: `/es/lugares/${slug}` },
  { nombre: "Mapa", ruta: "/es/mapa" },
  { nombre: "Agenda", ruta: "/es/agenda" },
];

const perfil = mkdtempSync(join(tmpdir(), "edge-lh-"));
const navegador = spawn(EDGE, [
  "--headless=new",
  "--remote-debugging-port=9350",
  `--user-data-dir=${perfil}`,
  "--no-first-run",
  "--disable-gpu",
  "about:blank",
]);

const esperar = (ms) => new Promise((r) => setTimeout(r, ms));
await esperar(3000);

const resultados = [];

for (const pagina of PAGINAS) {
  // El limite de peticiones se limpia antes de cada medida: las paginas
  // dinamicas piden al API y un 429 arruinaria la medicion.
  try {
    execSync(
      `docker compose exec -T redis sh -c "redis-cli --scan --pattern 'rate:*' | xargs -r redis-cli del"`,
      { cwd: RAIZ, stdio: "ignore" },
    );
  } catch {
    /* sin claves */
  }

  const resultado = await lighthouse(
    `${BASE}${pagina.ruta}`,
    {
      port: 9350,
      output: "json",
      logLevel: "error",
      // Movil, que es el publico principal de esta aplicacion y el escenario
      // mas exigente: red y CPU simuladas mas lentas.
      formFactor: "mobile",
      screenEmulation: { mobile: true, width: 412, height: 823, deviceScaleFactor: 1.75 },
      onlyCategories: ["performance", "accessibility", "best-practices", "seo"],
    },
  );

  const c = resultado.lhr.categories;
  const a = resultado.lhr.audits;

  resultados.push({
    pagina: pagina.nombre,
    rendimiento: Math.round(c.performance.score * 100),
    accesibilidad: Math.round(c.accessibility.score * 100),
    buenasPracticas: Math.round(c["best-practices"].score * 100),
    seo: Math.round(c.seo.score * 100),
    lcp: a["largest-contentful-paint"].numericValue,
    cls: a["cumulative-layout-shift"].numericValue,
    tbt: a["total-blocking-time"].numericValue,
    fallosA11y: c.accessibility.auditRefs
      .filter((ref) => a[ref.id]?.score !== null && a[ref.id]?.score < 1 && a[ref.id]?.scoreDisplayMode === "binary")
      .map((ref) => ref.id),
  });

  // Detalle de las auditorias que fallan: sin los elementos concretos no hay
  // forma de arreglarlas.
  for (const id of ["color-contrast", "heading-order", "aria-prohibited-attr"]) {
    const auditoria = a[id];
    if (auditoria && auditoria.score !== null && auditoria.score < 1) {
      const items = auditoria.details?.items ?? [];
      for (const item of items.slice(0, 3)) {
        console.log(
          `  [${pagina.nombre}] ${id}: ${(item.node?.snippet ?? "").slice(0, 120)}`,
        );
        if (item.node?.explanation) {
          console.log(`      ${item.node.explanation.slice(0, 160)}`);
        }
      }
    }
  }
  console.log(`  [${pagina.nombre}] TTFB: ${Math.round(a["server-response-time"]?.numericValue ?? 0)} ms | elemento LCP: ${(a["largest-contentful-paint-element"]?.details?.items?.[0]?.items?.[0]?.node?.snippet ?? "?").slice(0, 90)}`);
  console.log(`  medida: ${pagina.nombre}`);
}

console.log("\n  LIGHTHOUSE — build de produccion, emulacion movil");
console.log("  ".padEnd(84, "="));
console.log(
  "  " +
    "Pagina".padEnd(22) +
    "Rend.".padStart(7) +
    "A11y".padStart(7) +
    "BP".padStart(6) +
    "SEO".padStart(6) +
    "LCP".padStart(10) +
    "CLS".padStart(8) +
    "TBT".padStart(9),
);
console.log("  ".padEnd(84, "-"));

for (const r of resultados) {
  console.log(
    "  " +
      r.pagina.padEnd(22) +
      String(r.rendimiento).padStart(7) +
      String(r.accesibilidad).padStart(7) +
      String(r.buenasPracticas).padStart(6) +
      String(r.seo).padStart(6) +
      `${(r.lcp / 1000).toFixed(2)} s`.padStart(10) +
      r.cls.toFixed(3).padStart(8) +
      `${Math.round(r.tbt)} ms`.padStart(9),
  );
}
console.log("  ".padEnd(84, "="));

const bajo90 = resultados.filter(
  (r) => r.rendimiento < 90 || r.accesibilidad < 90 || r.buenasPracticas < 90 || r.seo < 90,
);
console.log(`  paginas con alguna categoria por debajo de 90: ${bajo90.length}`);

const clsAlto = resultados.filter((r) => r.cls >= 0.1);
const lcpAlto = resultados.filter((r) => r.lcp >= 2500);
console.log(`  CLS >= 0.1: ${clsAlto.length ? clsAlto.map((r) => r.pagina).join(", ") : "ninguna"}`);
console.log(`  LCP >= 2.5 s: ${lcpAlto.length ? lcpAlto.map((r) => r.pagina).join(", ") : "ninguna"}`);

const fallos = new Set(resultados.flatMap((r) => r.fallosA11y));
if (fallos.size) {
  console.log(`\n  auditorias de accesibilidad que fallan: ${[...fallos].join(", ")}`);
}

navegador.kill();
