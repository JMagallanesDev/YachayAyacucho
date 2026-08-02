import { NextResponse, type NextRequest } from "next/server";

/**
 * Proteccion de rutas privadas.
 *
 * En Next.js 16 esta convencion se llama `proxy.ts` (antes `middleware.ts`).
 *
 * IMPORTANTE — esto es experiencia de usuario, NO seguridad. Aqui solo puede
 * comprobarse si **existe** la cookie de refresh, no si es valida: su
 * contenido es un token opaco que se verifica contra la base de datos, y el
 * access token vive en memoria del navegador, fuera del alcance del servidor.
 *
 * Sirve para no mostrar el cascaron de una pagina privada a quien no ha
 * iniciado sesion. La autorizacion de verdad la hace el backend, con
 * `@PreAuthorize` en cada endpoint: si alguien fuerza la URL /admin, vera el
 * armazon vacio y todas las llamadas al API le responderan 403.
 *
 * ATENCION — esta comprobacion solo funciona cuando frontend y backend
 * comparten dominio, como en desarrollo. En el despliegue previsto (Vercel +
 * Railway) la cookie pertenece al dominio del backend y este proxy, que corre
 * en Vercel, **no podra verla**: dejara pasar a todo el mundo. Por eso las
 * paginas privadas llevan ademas la guarda de cliente `useSesionRequerida`,
 * que es la que realmente cubre ese caso.
 */

const COOKIE_REFRESH = "yachay_refresh";
const RUTAS_PRIVADAS = ["/perfil", "/admin"];

export function proxy(peticion: NextRequest) {
  const { pathname } = peticion.nextUrl;

  const esPrivada = RUTAS_PRIVADAS.some(
    (ruta) => pathname === ruta || pathname.startsWith(`${ruta}/`),
  );

  if (!esPrivada) {
    return NextResponse.next();
  }

  const tieneSesion = peticion.cookies.has(COOKIE_REFRESH);
  if (tieneSesion) {
    return NextResponse.next();
  }

  // Se recuerda a donde iba para devolverle alli tras iniciar sesion.
  const destino = new URL("/login", peticion.url);
  destino.searchParams.set("continuar", pathname);
  return NextResponse.redirect(destino);
}

export const config = {
  matcher: ["/perfil/:path*", "/admin/:path*"],
};
