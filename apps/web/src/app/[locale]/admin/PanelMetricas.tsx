"use client";

import dynamic from "next/dynamic";
import { useLocale, useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { ErrorApi } from "@/lib/auth";
import { metricas } from "@/lib/admin";
import type { Dashboard, PuntoDiario, Reparto } from "@/types/admin";

/**
 * Chart.js se carga solo aqui y solo en el navegador.
 *
 * <p>Son unos 200 KB que no tienen por que pagar quienes visitan el mapa o la
 * ficha de un lugar. Con {@code ssr: false} tampoco se intenta renderizar en el
 * servidor, que no tiene lienzo donde dibujar.</p>
 */
const GraficoSerie = dynamic(
  () => import("@/components/admin/Graficos").then((m) => m.GraficoSerie),
  { ssr: false, loading: () => <div className="h-56 w-full animate-pulse rounded-card bg-surface-muted" /> },
);

const GraficoReparto = dynamic(
  () => import("@/components/admin/Graficos").then((m) => m.GraficoReparto),
  { ssr: false, loading: () => <div className="h-36 w-full animate-pulse rounded-card bg-surface-muted" /> },
);

/**
 * Dashboard de metricas (RF-52).
 *
 * <p>Cada grafico va acompanado de <strong>su tabla</strong>, plegada. No es
 * adorno: un lienzo WebGL es invisible para un lector de pantalla y para quien
 * quiera copiar una cifra, y desplegar la tabla resuelve las dos cosas sin
 * cargar la pantalla.</p>
 */
export function PanelMetricas() {
  const t = useTranslations("panel");
  const idioma = useLocale();
  const { comprobando } = useSesionRequerida();

  const [datos, setDatos] = useState<Dashboard | null>(null);
  const [sinPermisos, setSinPermisos] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cargar = useCallback(() => metricas(idioma), [idioma]);

  useEffect(() => {
    if (comprobando) {
      return;
    }
    cargar()
      .then(setDatos)
      .catch((fallo) => {
        if (fallo instanceof ErrorApi && fallo.estado === 403) {
          setSinPermisos(true);
          return;
        }
        setError(t("errorCarga"));
      });
  }, [comprobando, cargar, t]);

  if (comprobando) {
    return null;
  }

  // Un usuario sin rol ADMIN ve esto, no el panel. Quien deniega de verdad es
  // el backend con un 403; esta pantalla solo lo cuenta.
  if (sinPermisos) {
    return (
      <p
        role="alert"
        data-testid="panel-sin-permisos"
        className="rounded-card bg-danger-subtle p-6 text-fluid-base text-text"
      >
        {t("sinPermisos")}
      </p>
    );
  }

  if (error) {
    return (
      <p role="alert" className="rounded-card bg-danger-subtle p-4 text-text">
        {error}
      </p>
    );
  }

  if (!datos) {
    return <p className="text-text-muted">{t("cargando")}</p>;
  }

  const totales = [
    { clave: "usuarios", valor: datos.totales.usuarios },
    { clave: "lugares", valor: datos.totales.lugares },
    { clave: "eventos", valor: datos.totales.eventos },
    { clave: "resenas", valor: datos.totales.resenas },
    { clave: "fotos", valor: datos.totales.fotos },
    { clave: "reportes", valor: datos.totales.reportes },
    { clave: "checkIns", valor: datos.totales.checkIns },
    { clave: "visitas", valor: datos.totales.visitasTotales },
  ];

  return (
    <section className="flex flex-col gap-6" data-testid="panel-metricas">
      <h2 className="text-fluid-xl font-semibold text-text">{t("metricas")}</h2>

      {/* ---- Totales ---------------------------------------------------- */}
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4" data-testid="totales">
        {totales.map((total) => (
          <div
            key={total.clave}
            data-testid={`total-${total.clave}`}
            data-valor={total.valor}
            className="flex flex-col gap-1 rounded-card border border-border-base bg-surface p-4"
          >
            <dt className="text-fluid-sm text-text-muted">{t(`total.${total.clave}`)}</dt>
            <dd className="text-fluid-2xl font-bold tabular-nums text-text">{total.valor}</dd>
          </div>
        ))}
      </dl>

      {/* ---- Series temporales ------------------------------------------ */}
      <div className="grid gap-4 lg:grid-cols-2">
        <Tarjeta titulo={t("visitasPorDia")} descripcion={t("visitasPorDiaAyuda")}>
          <GraficoSerie puntos={datos.visitas} etiquetaSerie={t("visitasPorDia")} />
          <TablaDiaria puntos={datos.visitas} etiqueta={t("visitasPorDia")} />
        </Tarjeta>

        <Tarjeta titulo={t("registrosPorDia")} descripcion={t("registrosPorDiaAyuda")}>
          {/* Barras y no linea: los registros son sucesos sueltos y muchos dias
              valen cero; una linea uniria los ceros y fingiria continuidad. */}
          <GraficoSerie puntos={datos.registros} etiquetaSerie={t("registrosPorDia")} comoBarras />
          <TablaDiaria puntos={datos.registros} etiqueta={t("registrosPorDia")} />
        </Tarjeta>
      </div>

      {/* ---- Repartos ---------------------------------------------------- */}
      <div className="grid gap-4 lg:grid-cols-2">
        <Tarjeta titulo={t("lugaresPorCategoria")}>
          {datos.lugaresPorCategoria.length === 0 ? (
            <p className="text-fluid-sm text-text-muted">{t("sinDatos")}</p>
          ) : (
            <>
              <GraficoReparto items={datos.lugaresPorCategoria} />
              <TablaReparto items={datos.lugaresPorCategoria} etiqueta={t("lugaresPorCategoria")} />
            </>
          )}
        </Tarjeta>

        <Tarjeta titulo={t("visitasPorSeccion")}>
          {datos.visitasPorSeccion.length === 0 ? (
            <p className="text-fluid-sm text-text-muted" data-testid="sin-visitas">
              {t("sinDatos")}
            </p>
          ) : (
            <>
              <GraficoReparto items={datos.visitasPorSeccion} />
              <TablaReparto items={datos.visitasPorSeccion} etiqueta={t("visitasPorSeccion")} />
            </>
          )}
        </Tarjeta>
      </div>
    </section>
  );
}

function Tarjeta({
  titulo,
  descripcion,
  children,
}: {
  titulo: string;
  descripcion?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-5">
      <div className="flex flex-col gap-1">
        <h3 className="text-fluid-base font-semibold text-text">{titulo}</h3>
        {descripcion && <p className="text-fluid-sm text-text-muted">{descripcion}</p>}
      </div>
      {children}
    </section>
  );
}

/** Los mismos datos del grafico, en tabla, para quien no ve el lienzo. */
function TablaDiaria({ puntos, etiqueta }: { puntos: PuntoDiario[]; etiqueta: string }) {
  const t = useTranslations("panel");
  return (
    <details className="text-fluid-sm">
      <summary className="cursor-pointer text-text-muted">{t("verTabla")}</summary>
      <div className="mt-2 max-h-48 overflow-y-auto">
        <table className="w-full text-start">
          <caption className="sr-only">{etiqueta}</caption>
          <thead>
            <tr className="text-text-muted">
              <th scope="col" className="py-1">{t("fecha")}</th>
              <th scope="col" className="py-1 text-end">{t("valor")}</th>
            </tr>
          </thead>
          <tbody>
            {puntos.map((punto) => (
              <tr key={punto.fecha} className="border-t border-border-base">
                <td className="py-1 text-text">{punto.fecha}</td>
                <td className="py-1 text-end tabular-nums text-text">{punto.valor}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  );
}

function TablaReparto({ items, etiqueta }: { items: Reparto[]; etiqueta: string }) {
  const t = useTranslations("panel");
  return (
    <details className="text-fluid-sm">
      <summary className="cursor-pointer text-text-muted">{t("verTabla")}</summary>
      <table className="mt-2 w-full text-start">
        <caption className="sr-only">{etiqueta}</caption>
        <tbody>
          {items.map((item) => (
            <tr key={item.etiqueta} className="border-t border-border-base">
              <td className="py-1 text-text">{item.etiqueta}</td>
              <td className="py-1 text-end tabular-nums text-text">{item.valor}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </details>
  );
}
