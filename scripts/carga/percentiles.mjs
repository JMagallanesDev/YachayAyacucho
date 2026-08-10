/** Percentiles por endpoint a partir de un .jtl de JMeter. */
import fs from "node:fs";

const archivo = process.argv[2];
const etiqueta = process.argv[3] ?? "";

const lineas = fs.readFileSync(archivo, "utf8").trim().split("\n");
const cabecera = lineas[0].split(",");
const iElapsed = cabecera.indexOf("elapsed");
const iLabel = cabecera.indexOf("label");
const iCodigo = cabecera.indexOf("responseCode");

const porEtiqueta = new Map();
const todos = [];
let errores = 0;

for (const linea of lineas.slice(1)) {
  const c = linea.split(",");
  const ms = Number(c[iElapsed]);
  const label = c[iLabel];
  if (Number.isNaN(ms)) continue;

  if (c[iCodigo] !== "200") errores++;

  todos.push(ms);
  if (!porEtiqueta.has(label)) porEtiqueta.set(label, []);
  porEtiqueta.get(label).push(ms);
}

function percentil(valores, p) {
  const ordenados = [...valores].sort((a, b) => a - b);
  const i = Math.ceil((p / 100) * ordenados.length) - 1;
  return ordenados[Math.max(0, i)];
}

console.log(`\n  ${etiqueta}`);
console.log("  ".padEnd(72, "="));
console.log(
  "  " + "Endpoint".padEnd(34) + "n".padStart(7) + "media".padStart(8) + "P95".padStart(8) + "P99".padStart(8),
);
console.log("  ".padEnd(72, "-"));

for (const [label, valores] of [...porEtiqueta].sort()) {
  const media = valores.reduce((s, v) => s + v, 0) / valores.length;
  console.log(
    "  " +
      label.padEnd(34) +
      String(valores.length).padStart(7) +
      `${media.toFixed(0)} ms`.padStart(8) +
      `${percentil(valores, 95)} ms`.padStart(8) +
      `${percentil(valores, 99)} ms`.padStart(8),
  );
}

const p95 = percentil(todos, 95);
console.log("  ".padEnd(72, "-"));
console.log(
  "  " +
    "TOTAL".padEnd(34) +
    String(todos.length).padStart(7) +
    `${(todos.reduce((s, v) => s + v, 0) / todos.length).toFixed(0)} ms`.padStart(8) +
    `${p95} ms`.padStart(8) +
    `${percentil(todos, 99)} ms`.padStart(8),
);
console.log("  ".padEnd(72, "="));
console.log(`  errores: ${errores} de ${todos.length}`);
console.log(`  P95 global: ${p95} ms  ->  ${p95 < 500 ? "CUMPLE" : "NO CUMPLE"} el objetivo de 500 ms\n`);
