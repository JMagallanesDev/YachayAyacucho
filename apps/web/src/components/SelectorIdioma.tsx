"use client";

import { useLocale, useTranslations } from "next-intl";
import { useTransition } from "react";

import { usePathname, useRouter } from "@/i18n/navegacion";
import { idiomas, type Idioma } from "@/i18n/routing";

/**
 * Cambio de idioma conservando la pagina actual (RF-59, RF-63).
 *
 * <p>Usa el router de next-intl y no el de Next a secas: ademas de poner el
 * prefijo correcto, es lo que **escribe la cookie NEXT_LOCALE**. Sin esa
 * cookie el cambio funciona en la pagina actual pero no se recuerda, y la
 * siguiente visita vuelve al idioma que detecte el navegador.</p>
 *
 * <p>{@code usePathname} de next-intl devuelve la ruta **sin** el prefijo de
 * idioma, asi que pasarle el idioma destino basta para reconstruir la URL:
 * quien esta leyendo una ficha y cambia de idioma sigue en esa ficha.</p>
 *
 * <p>La navegacion va dentro de `useTransition` para que la interfaz no se
 * bloquee mientras el servidor devuelve la version traducida (RNF-05).</p>
 */
export function SelectorIdioma() {
  const t = useTranslations("comun");
  const idiomaActual = useLocale();
  const router = useRouter();
  const ruta = usePathname();
  const [enTransicion, iniciarTransicion] = useTransition();

  function cambiar(destino: Idioma) {
    if (destino === idiomaActual) {
      return;
    }
    iniciarTransicion(() => {
      router.replace(ruta, { locale: destino });
    });
  }

  return (
    <nav
      aria-label={t("cambiarIdioma")}
      className="flex justify-end gap-1 px-5 pt-safe"
      data-testid="selector-idioma"
    >
      {idiomas.map((idioma) => {
        const activo = idioma === idiomaActual;
        return (
          <button
            key={idioma}
            type="button"
            onClick={() => cambiar(idioma)}
            disabled={enTransicion}
            aria-current={activo ? "true" : undefined}
            lang={idioma}
            className={`press min-h-touch rounded-card px-3 text-fluid-sm font-medium ${
              activo ? "bg-primary text-primary-fg" : "text-text-muted"
            }`}
          >
            {idioma === "es" ? t("espanol") : t("ingles")}
          </button>
        );
      })}
    </nav>
  );
}
