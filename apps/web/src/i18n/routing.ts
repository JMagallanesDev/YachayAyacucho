import { defineRouting } from "next-intl/routing";

/**
 * Enrutado por idioma (RF-59, RF-60, RF-64).
 *
 * <p>Espanol e ingles se traducen. Frances y aleman se detectan pero **caen a
 * ingles**, no a espanol: quien navega en frances entiende antes el ingles que
 * el castellano. Es lo que fija el alcance del proyecto.</p>
 */
export const idiomas = ["es", "en"] as const;
export type Idioma = (typeof idiomas)[number];

/** Idiomas que se detectan sin traducirse, con su destino. */
export const IDIOMAS_CON_FALLBACK: Record<string, Idioma> = {
  fr: "en",
  de: "en",
};

export const routing = defineRouting({
  locales: idiomas,
  defaultLocale: "es",

  // Prefijo siempre visible (/es/..., /en/...). Con 'as-needed' el espanol
  // viviria en URLs sin prefijo y las dos versiones de cada ficha no serian
  // simetricas, lo que complica el SEO y la revalidacion ISR por idioma.
  localePrefix: "always",

  // next-intl guarda el idioma elegido en una cookie.
  //
  // No en localStorage, aunque el RF-63 lo dijera: localStorage no existe en
  // el servidor, de modo que no puede decidir el idioma antes de renderizar
  // ni redirigir correctamente a quien entra por "/". La cookie viaja en la
  // peticion y cumple la misma promesa de persistencia.
  localeCookie: {
    name: "NEXT_LOCALE",
    maxAge: 60 * 60 * 24 * 365,
    sameSite: "lax",
  },
});
