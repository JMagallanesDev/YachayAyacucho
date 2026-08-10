import path from "node:path";
import createNextIntlPlugin from "next-intl/plugin";
import type { NextConfig } from "next";

/**
 * Carga del .env unico de la raiz del monorepo.
 *
 * Next.js solo busca archivos .env dentro de su propia carpeta, asi que hay
 * que traerlo a mano. `process.loadEnvFile` es API nativa de Node (>= 20.12),
 * por lo que no hace falta dotenv ni ninguna dependencia.
 *
 * Las variables que ya existan en el entorno real tienen prioridad sobre las
 * del archivo, que es justo lo que se necesita en Vercel: alli manda el panel
 * del proveedor y este archivo ni siquiera existe.
 */
const rootEnvPath = path.resolve(process.cwd(), "../../.env");

try {
  process.loadEnvFile(rootEnvPath);
} catch {
  console.warn(
    `[env] No se pudo leer ${rootEnvPath}. Se usaran solo las variables del entorno del sistema.`,
  );
}

/**
 * Variables que deben viajar al navegador.
 *
 * Cargarlas en `process.env` desde next.config NO basta. Next construye su
 * propia lista de variables `NEXT_PUBLIC_*` a inyectar en el bundle de cliente
 * leyendo los archivos .env que encuentra en la carpeta de la aplicacion, y
 * esa lista se fija con independencia de lo que este archivo anada despues.
 *
 * El resultado era una diferencia silenciosa entre entornos: `next build` si
 * sustituia `process.env.NEXT_PUBLIC_API_URL` por su valor, pero `next dev` la
 * dejaba sin sustituir. En el navegador quedaba `undefined`, la validacion de
 * `lib/env.ts` lanzaba al evaluar el modulo y la pagina no llegaba a
 * hidratarse: los formularios quedaban inertes.
 *
 * La clave `env` de la configuracion es el mecanismo documentado para esto:
 * declara explicitamente que sustituir, y funciona igual en desarrollo, en
 * produccion y en Vercel.
 */
const VARIABLES_PUBLICAS = [
  "NEXT_PUBLIC_API_URL",
  "NEXT_PUBLIC_APP_NAME",
  "NEXT_PUBLIC_DEFAULT_LOCALE",
  "NEXT_PUBLIC_MAPTILER_KEY",
  "NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME",
  "NEXT_PUBLIC_SITE_URL",
] as const;

const variablesPublicas: Record<string, string> = Object.fromEntries(
  VARIABLES_PUBLICAS.filter((clave) => process.env[clave] !== undefined).map((clave) => [
    clave,
    process.env[clave] as string,
  ]),
);

// Aviso temprano y explicito: sin la URL del API no hay aplicacion que valga,
// y es preferible verlo al arrancar que descubrirlo con un formulario muerto.
if (!variablesPublicas.NEXT_PUBLIC_API_URL) {
  console.warn(
    "[env] NEXT_PUBLIC_API_URL no esta definida. Revisa el .env de la raiz del proyecto.",
  );
}

const nextConfig: NextConfig = {
  reactStrictMode: true,

  /**
   * Imagenes remotas permitidas (Bloque 13).
   *
   * Next exige declarar los dominios de los que aceptara imagenes: sin esta
   * lista, `next/image` rechaza cualquier URL externa. Son los tres origenes
   * reales del proyecto y ninguno mas, que es justo la idea — un comodin
   * convertiria el optimizador en un proxy abierto para cualquier imagen de
   * internet.
   */
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "res.cloudinary.com" },
      { protocol: "https", hostname: "i.ytimg.com" },
      { protocol: "https", hostname: "openweathermap.org" },
    ],
  },

  // No anunciar el framework al mundo.
  poweredByHeader: false,

  env: variablesPublicas,

  // La raiz del monorepo, fijada a mano. Sin esto Next la deduce buscando
  // lockfiles hacia arriba y puede escoger una carpeta ajena al proyecto, lo
  // que ensucia el rastreo de archivos del despliegue.
  outputFileTracingRoot: path.resolve(process.cwd(), "../.."),
};

// El plugin conecta la configuracion de next-intl (src/i18n/request.ts) con
// el compilador, para que los mensajes se resuelvan en el servidor.
const conIntl = createNextIntlPlugin("./src/i18n/request.ts");

export default conIntl(nextConfig);
