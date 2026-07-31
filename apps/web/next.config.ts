import path from "node:path";
import type { NextConfig } from "next";

/**
 * Carga del .env unico de la raiz del monorepo.
 *
 * Next.js solo busca archivos .env dentro de su propia carpeta, asi que
 * hay que traerlo a mano. `process.loadEnvFile` es API nativa de Node
 * (>= 20.12), por lo que no hace falta dotenv ni ninguna dependencia.
 *
 * Se ejecuta aqui a proposito: Next evalua next.config.ts antes de
 * compilar, de modo que las variables NEXT_PUBLIC_* ya estan disponibles
 * cuando se inyectan en el bundle del navegador.
 *
 * Las variables que ya existan en el entorno real tienen prioridad sobre
 * las del archivo, que es justo lo que se necesita en Vercel: alli manda
 * el panel del proveedor y este archivo ni siquiera existe.
 */
const rootEnvPath = path.resolve(process.cwd(), "../../.env");

try {
  process.loadEnvFile(rootEnvPath);
} catch {
  console.warn(
    `[env] No se pudo leer ${rootEnvPath}. Se usaran solo las variables del entorno del sistema.`,
  );
}

const nextConfig: NextConfig = {
  reactStrictMode: true,

  // No anunciar el framework al mundo.
  poweredByHeader: false,

  // La raiz del monorepo, fijada a mano. Sin esto Next la deduce
  // buscando lockfiles hacia arriba y puede escoger una carpeta ajena
  // al proyecto, lo que ensucia el rastreo de archivos del despliegue.
  outputFileTracingRoot: path.resolve(process.cwd(), "../.."),
};

export default nextConfig;
