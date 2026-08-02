import { hasLocale } from "next-intl";
import { getRequestConfig } from "next-intl/server";

import { routing } from "./routing";

/**
 * Carga los mensajes del idioma activo en cada peticion del servidor.
 *
 * <p>Solo se carga el idioma en uso, no los dos: es lo que mantiene el bundle
 * de traduccion por debajo de los 50 KB que exige el RNF-06.</p>
 */
export default getRequestConfig(async ({ requestLocale }) => {
  const solicitado = await requestLocale;
  const locale = hasLocale(routing.locales, solicitado) ? solicitado : routing.defaultLocale;

  return {
    locale,
    messages: (await import(`../mensajes/${locale}.json`)).default,
    // Zona horaria fija: las fechas de eventos y horarios son de Ayacucho,
    // no del dispositivo del visitante (RF-67).
    timeZone: "America/Lima",
  };
});
