/**
 * Variables de entorno del frontend, validadas en un unico lugar.
 *
 * Importante: cada variable se lee con su nombre literal
 * (`process.env.NEXT_PUBLIC_API_URL`) y nunca con indice dinamico
 * (`process.env[nombre]`). Next.js sustituye estas expresiones por su
 * valor en tiempo de compilacion buscando el texto exacto; con un
 * indice dinamico la sustitucion no ocurre y la variable llegaria
 * vacia al navegador.
 */

function requerida(valor: string | undefined, nombre: string): string {
  if (!valor) {
    throw new Error(
      `Falta la variable de entorno ${nombre}. Revisa el archivo .env de la raiz del proyecto (copialo de .env.example).`,
    );
  }
  return valor;
}

export const env = {
  /** URL base del API Spring Boot, ya con el prefijo /api/v1. */
  apiUrl: requerida(process.env.NEXT_PUBLIC_API_URL, "NEXT_PUBLIC_API_URL"),

  /**
   * Clave de MapTiler. Es PUBLICA por diseno: viaja con cada peticion de tile
   * y no hay forma de ocultarla en un mapa de cliente. Su proteccion no es el
   * secreto sino el ambito: en el panel de MapTiler se restringe por origenes
   * HTTP permitidos, de modo que solo funcione desde nuestro dominio.
   *
   * No es `requerida()` a proposito: sin ella el mapa muestra un aviso de
   * configuracion y el resto del sitio funciona igual.
   */
  maptilerKey: process.env.NEXT_PUBLIC_MAPTILER_KEY ?? "",

  appName: process.env.NEXT_PUBLIC_APP_NAME ?? "Yachay Ayacucho",

  /**
   * URL publica del sitio, sin barra final.
   *
   * <p>La necesitan el sitemap, las canonicas y Open Graph. No es
   * `requerida()` porque en desarrollo el valor por defecto sirve, pero sin
   * ella en produccion las redes sociales reciben URLs relativas y no
   * resuelven ninguna imagen.</p>
   */
  siteUrl: (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/$/, ""),

  defaultLocale: process.env.NEXT_PUBLIC_DEFAULT_LOCALE ?? "es",
} as const;
