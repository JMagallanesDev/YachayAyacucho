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

  appName: process.env.NEXT_PUBLIC_APP_NAME ?? "Yachay Ayacucho",

  defaultLocale: process.env.NEXT_PUBLIC_DEFAULT_LOCALE ?? "es",
} as const;
