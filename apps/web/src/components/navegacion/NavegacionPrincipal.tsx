"use client";

import { motion, useReducedMotion } from "motion/react";
import { useTranslations } from "next-intl";
import { usePathname } from "next/navigation";

import { InterruptorTema } from "@/components/tema/InterruptorTema";
import { Link } from "@/i18n/navegacion";

/**
 * Los cinco destinos. Cinco es el maximo antes de que las etiquetas dejen de
 * leerse en un movil de 320px, asi que la eleccion es excluyente: entran los
 * que se visitan a diario. El directorio de negocios y la agenda de
 * preservacion se alcanzan desde sus paginas madre.
 */
const DESTINOS = [
  { clave: "inicio", href: "/", icono: "⌂" },
  { clave: "lugares", href: "/lugares", icono: "◫" },
  { clave: "mapa", href: "/mapa", icono: "◉" },
  { clave: "agenda", href: "/agenda", icono: "▦" },
  { clave: "perfil", href: "/perfil", icono: "○" },
] as const;

/**
 * Navegacion principal (Bloque 12).
 *
 * <p><strong>El mismo componente en dos formas.</strong> En movil es una barra
 * inferior fija —el pulgar llega abajo, no arriba— y en escritorio sube a una
 * barra superior. No son dos componentes: es el mismo con las utilidades
 * responsive de Tailwind, porque duplicarlo garantizaria que uno de los dos se
 * quedara sin actualizar.</p>
 *
 * <p><strong>El indicador se desliza con {@code layoutId}.</strong> Motion mide
 * la posicion antes y despues y anima la diferencia; sin eso el fondo del
 * destino activo aparecerian y desaparecerian de golpe, que es exactamente la
 * sensacion de web que este bloque intenta quitar.</p>
 *
 * <p>Se apoya en indigo y nunca en carmin: el carmin es <em>accion</em> y la
 * navegacion es <em>estructura</em>. Si compartieran color, el ojo dejaria de
 * distinguir «pulsa esto» de «estas aqui».</p>
 */
export function NavegacionPrincipal() {
  const t = useTranslations("navegacion");
  const ruta = usePathname();
  const reducido = useReducedMotion();

  // El pathname llega con el idioma delante (/es/lugares). Se le quita para
  // comparar contra los href, que son sin idioma.
  const sinIdioma = ruta.replace(/^\/(es|en)(?=\/|$)/, "") || "/";

  const esActivo = (href: string) =>
    href === "/" ? sinIdioma === "/" : sinIdioma.startsWith(href);

  return (
    <nav
      aria-label={t("etiqueta")}
      data-testid="navegacion-principal"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-nav-border bg-nav-bg backdrop-blur-md
                 md:inset-x-0 md:top-0 md:bottom-auto md:border-t-0 md:border-b"
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
    >
      <div className="mx-auto flex w-full max-w-3xl items-center justify-between gap-2 px-2 md:px-5">
        {/* La marca solo en escritorio: en movil el espacio es para los
            destinos, y el nombre ya esta en la pestana del navegador. */}
        <Link
          href="/"
          className="hidden shrink-0 font-display text-fluid-lg font-bold text-nav-item-activo md:block"
        >
          Yachay
        </Link>

        <ul className="flex flex-1 items-stretch justify-around md:flex-none md:justify-start md:gap-1">
          {DESTINOS.map((destino) => {
            const activo = esActivo(destino.href);
            return (
              <li key={destino.clave} className="flex">
                <Link
                  href={destino.href}
                  aria-current={activo ? "page" : undefined}
                  data-testid={`nav-${destino.clave}`}
                  data-activo={activo}
                  className="press relative flex min-h-nav min-w-touch flex-col items-center justify-center gap-0.5
                             rounded-card px-3 py-1.5 md:flex-row md:gap-2"
                >
                  {activo && (
                    <motion.span
                      layoutId="indicador-navegacion"
                      aria-hidden="true"
                      className="absolute inset-x-1 inset-y-0.5 rounded-card bg-nav-indicador"
                      transition={
                        reducido
                          ? { duration: 0 }
                          : { type: "spring", stiffness: 400, damping: 32 }
                      }
                    />
                  )}
                  <span
                    aria-hidden="true"
                    className={`relative text-fluid-lg leading-none ${
                      activo ? "text-nav-item-activo" : "text-nav-item"
                    }`}
                  >
                    {destino.icono}
                  </span>
                  <span
                    className={`relative text-[0.7rem] leading-tight font-medium md:text-fluid-sm ${
                      activo ? "text-nav-item-activo" : "text-nav-item"
                    }`}
                  >
                    {t(destino.clave)}
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>

        {/* El interruptor de tema vive en la barra: es la posicion donde se
            busca, y asi esta disponible desde cualquier pagina. En movil no
            cabe junto a cinco destinos, asi que se muestra solo en escritorio;
            en movil vive en /perfil. */}
        <div className="hidden shrink-0 md:block">
          <InterruptorTema />
        </div>
      </div>
    </nav>
  );
}
