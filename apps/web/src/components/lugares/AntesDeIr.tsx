import { useFormatter, useTranslations } from "next-intl";

import type { LugarDetalle } from "@/types/lugar";

/**
 * Bloque "Antes de ir" (RF-09d).
 *
 * <p>Server Component: son datos estaticos del lugar, no necesitan JavaScript
 * en el navegador y viajan ya renderizados dentro del HTML de la ficha.</p>
 *
 * <p>Solo se muestra lo que se sabe. Un dato desconocido se omite en lugar de
 * pintarse como "no": decirle a alguien en silla de ruedas que un sitio no es
 * accesible cuando en realidad no se ha comprobado es peor que no decir nada.</p>
 */
export function AntesDeIr({ lugar }: { lugar: LugarDetalle }) {
  const t = useTranslations("lugares");
  const formato = useFormatter();

  const datos: { clave: string; etiqueta: string; valor: string }[] = [];

  if (lugar.precioEntradaPen !== null) {
    const precio = Number(lugar.precioEntradaPen);
    datos.push({
      clave: "precio",
      etiqueta: t("precio"),
      valor: precio === 0 ? t("gratis") : formato.number(precio, { style: "currency", currency: "PEN" }),
    });
  }

  if (lugar.duracionVisitaMin !== null) {
    datos.push({
      clave: "duracion",
      etiqueta: t("duracion"),
      valor: t("minutos", { minutos: lugar.duracionVisitaMin }),
    });
  }

  if (lugar.costoTaxiDesdePlazaPen !== null) {
    datos.push({
      clave: "taxi",
      etiqueta: t("taxiDesdePlaza"),
      valor: formato.number(Number(lugar.costoTaxiDesdePlazaPen), {
        style: "currency",
        currency: "PEN",
      }),
    });
  }

  const condiciones: { clave: string; valor: boolean | null }[] = [
    { clave: "aceptaTarjeta", valor: lugar.aceptaTarjeta },
    { clave: "tieneBanos", valor: lugar.tieneBanos },
    { clave: "accesibleSillaRuedas", valor: lugar.accesibleSillaRuedas },
    { clave: "aptoNinos", valor: lugar.aptoNinos },
    { clave: "requiereGuia", valor: lugar.requiereGuia },
  ];

  for (const condicion of condiciones) {
    if (condicion.valor !== null) {
      datos.push({
        clave: condicion.clave,
        etiqueta: t(condicion.clave),
        valor: condicion.valor ? t("si") : t("no"),
      });
    }
  }

  if (datos.length === 0) {
    return null;
  }

  return (
    <section data-testid="antes-de-ir" className="flex flex-col gap-4">
      <h2 className="text-fluid-xl font-semibold text-text">{t("antesDeIr")}</h2>
      <dl className="grid gap-3 sm:grid-cols-2">
        {datos.map((dato) => (
          <div
            key={dato.clave}
            className="flex items-baseline justify-between gap-3 rounded-card bg-surface-muted px-4 py-3"
          >
            <dt className="text-fluid-sm text-text-muted">{dato.etiqueta}</dt>
            <dd className="text-fluid-sm font-medium text-text">{dato.valor}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
