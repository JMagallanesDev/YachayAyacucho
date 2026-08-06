"use client";

import { useTranslations } from "next-intl";

/**
 * Deep links a las aplicaciones de navegacion (RF-21).
 *
 * <p>Se ofrecen las tres siempre, sin detectar el sistema para esconder
 * ninguna: en escritorio los enlaces abren la version web, y adivinar mal el
 * dispositivo dejaria a alguien sin la opcion que usa. Los tres formatos son
 * URLs universales documentadas por cada proveedor, que el sistema
 * operativo redirige a la aplicacion nativa si esta instalada.</p>
 */
export function AbrirEn({
  longitud,
  latitud,
  nombre,
}: {
  longitud: number;
  latitud: number;
  nombre: string;
}) {
  const t = useTranslations("mapa");
  const etiqueta = encodeURIComponent(nombre);

  const destinos = [
    {
      clave: "googleMaps",
      url: `https://www.google.com/maps/search/?api=1&query=${latitud},${longitud}`,
    },
    {
      clave: "waze",
      url: `https://waze.com/ul?ll=${latitud},${longitud}&navigate=yes`,
    },
    {
      clave: "appleMaps",
      url: `https://maps.apple.com/?ll=${latitud},${longitud}&q=${etiqueta}`,
    },
  ];

  return (
    <div className="flex flex-wrap gap-2" data-testid="abrir-en">
      {destinos.map((destino) => (
        <a
          key={destino.clave}
          href={destino.url}
          target="_blank"
          // noreferrer ademas de noopener: no hay motivo para contarle a Google
          // o a Waze desde que pagina exacta se llego.
          rel="noopener noreferrer"
          data-testid={`abrir-${destino.clave}`}
          className="press rounded-card border border-border-strong px-2.5 py-1 text-xs font-medium text-text"
        >
          {t(destino.clave)}
        </a>
      ))}
      <span className="sr-only">{t("abrirEnAyuda")}</span>
    </div>
  );
}
