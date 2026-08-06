import { useFormatter, useTranslations } from "next-intl";

import type { Clima } from "@/types/clima";

/**
 * Clima actual con sus consejos (RF-25, RF-27).
 *
 * <p>Server Component: es un dato que ya viene resuelto y no necesita
 * JavaScript en el navegador.</p>
 *
 * <p>Lo importante de este componente no es el caso feliz, sino los otros dos.
 * Cuando el proveedor no responde, la tarjeta muestra el ultimo dato conocido
 * <strong>diciendo cuando se midio</strong>; y cuando no hay nada, no se pinta
 * en absoluto en vez de dejar un hueco con guiones. Mostrar una temperatura
 * vieja como si fuera actual seria mentir sobre algo que se usa para decidir si
 * salir con abrigo.</p>
 */
export function TarjetaClima({ clima }: { clima: Clima }) {
  const t = useTranslations("clima");
  const formato = useFormatter();

  if (!clima.disponible) {
    return null;
  }

  return (
    <section
      data-testid="tarjeta-clima"
      data-obsoleto={clima.obsoleto}
      className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-5 shadow-card"
    >
      <div className="flex items-center justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h2 className="text-fluid-sm font-medium text-text-muted">{t("titulo")}</h2>
          <p className="text-fluid-3xl font-bold text-text" data-testid="temperatura">
            {clima.temperatura !== null
              ? formato.number(clima.temperatura, { maximumFractionDigits: 0 })
              : "—"}
            <span className="text-fluid-lg font-medium text-text-muted">°C</span>
          </p>
          {clima.condicion && (
            <p className="text-fluid-sm text-text-muted">{t(`condicion.${clima.condicion}`)}</p>
          )}
        </div>

        {clima.icono && (
          // Icono del propio OpenWeatherMap. Es la unica peticion que el
          // navegador hace a su dominio, y no lleva ninguna clave.
          //
          // Se usa <img> y no next/image a proposito: son 80x80 px que ya
          // llegan optimizados desde su CDN, y pasarlos por el optimizador de
          // Next anadiria una transformacion en el servidor —con su coste— para
          // no ahorrar nada. Al ser decorativo, su alt va vacio.
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={`https://openweathermap.org/img/wn/${clima.icono}@2x.png`}
            alt=""
            width={80}
            height={80}
            className="size-20"
          />
        )}
      </div>

      {/* Honestidad sobre la antiguedad del dato: aparece solo si hace falta. */}
      {clima.obsoleto && clima.medidoEn && (
        <p
          data-testid="aviso-clima-obsoleto"
          className="rounded-card bg-surface-muted px-3 py-2 text-fluid-sm text-text-muted"
        >
          {t("obsoleto", {
            cuando: formato.relativeTime(new Date(clima.medidoEn)),
          })}
        </p>
      )}

      {clima.consejos.length > 0 && (
        <ul className="flex flex-col gap-1.5" data-testid="consejos-clima">
          {clima.consejos.map((consejo) => (
            <li key={consejo} className="flex gap-2 text-fluid-sm text-text">
              <span aria-hidden="true">·</span>
              {t(`consejo.${consejo}`)}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
