import createIntlMiddleware from "next-intl/middleware";
import { NextResponse, type NextRequest } from "next/server";

import { routing } from "@/i18n/routing";

/**
 * Enrutado por idioma y proteccion de rutas privadas.
 *
 * En Next.js 16 esta convencion se llama `proxy.ts` (antes `middleware.ts`).
 *
 * El orden importa: primero se resuelve el idioma, porque hasta que next-intl
 * no normaliza la URL no se sabe si `/perfil` es en realidad `/es/perfil`. Con
 * la ruta ya normalizada se decide si es privada.
 *
 * IMPORTANTE — la parte de proteccion es experiencia de usuario, NO seguridad.
 * Aqui solo puede comprobarse si **existe** la cookie de refresh, no si es
 * valida. Y en el despliegue previsto (Vercel + Railway) la cookie pertenece
 * al dominio del backend, asi que este proxy ni siquiera la vera: quien
 * protege de verdad es `useSesionRequerida` en el cliente y `@PreAuthorize`
 * en el backend.
 */

const COOKIE_REFRESH = "yachay_refresh";
const RUTAS_PRIVADAS = ["/perfil", "/admin"];

const intl = createIntlMiddleware(routing);

export function proxy(peticion: NextRequest) {
  // 1. Idioma: puede devolver una redireccion (por ejemplo de "/" a "/es").
  const respuesta = intl(peticion);

  const { pathname } = peticion.nextUrl;
  // Se quita el prefijo de idioma para comparar contra las rutas privadas.
  const sinIdioma = pathname.replace(/^\/(es|en)(?=\/|$)/, "") || "/";

  const esPrivada = RUTAS_PRIVADAS.some(
    (ruta) => sinIdioma === ruta || sinIdioma.startsWith(`${ruta}/`),
  );

  if (!esPrivada || peticion.cookies.has(COOKIE_REFRESH)) {
    return respuesta;
  }

  // 2. Sin sesion en una ruta privada: a /login, conservando el idioma de la
  // URL para no devolver al usuario a otro idioma del que estaba usando.
  const idioma = pathname.match(/^\/(es|en)(?=\/|$)/)?.[1] ?? routing.defaultLocale;
  const destino = new URL(`/${idioma}/login`, peticion.url);
  destino.searchParams.set("continuar", pathname);
  return NextResponse.redirect(destino);
}

export const config = {
  // Se excluyen los endpoints de API, los archivos internos de Next y
  // cualquier ruta con extension: nada de eso necesita idioma ni sesion.
  matcher: ["/((?!api|_next|_vercel|.*\\..*).*)"],
};
