"use client";

import { useEffect, useState } from "react";
import { useLocale, useTranslations } from "next-intl";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { Link } from "@/i18n/navegacion";
import { misNegocios } from "@/lib/negocios";
import type { MiNegocio } from "@/types/negocio";

/**
 * Panel del dueno de negocio (RF-107).
 *
 * <p>Muestra sus negocios en cualquier estado —incluidos los pendientes y los
 * rechazados, con su motivo— y las metricas agregadas de cada uno.</p>
 *
 * <p><strong>Las metricas son solo numeros.</strong> Cuanta gente abrio la
 * ficha y cuantos pulsaron WhatsApp, nunca quienes: el backend no guarda esa
 * informacion, asi que esta pantalla no podria mostrarla aunque quisiera. Es la
 * misma linea del anonimato del Bloque 8.</p>
 */
export function MisNegocios() {
  const t = useTranslations("miNegocio");
  const idioma = useLocale();
  const { comprobando } = useSesionRequerida();

  const [negocios, setNegocios] = useState<MiNegocio[] | null>(null);

  useEffect(() => {
    if (comprobando) {
      return;
    }
    misNegocios(idioma).then(setNegocios).catch(() => setNegocios([]));
  }, [comprobando, idioma]);

  if (comprobando || negocios === null) {
    return <p className="text-text-muted">{t("cargando")}</p>;
  }

  if (negocios.length === 0) {
    return (
      <div className="flex flex-col gap-3 rounded-card bg-surface-muted p-6">
        <p data-testid="sin-negocios-propios" className="text-fluid-base text-text-muted">
          {t("sinNegocios")}
        </p>
        <Link
          href="/negocios/registrar"
          className="press min-h-touch w-fit rounded-card bg-primary px-5 py-2 text-fluid-sm font-medium text-primary-fg"
        >
          {t("registrarUno")}
        </Link>
      </div>
    );
  }

  return (
    <ul className="flex flex-col gap-5" data-testid="mis-negocios">
      {negocios.map(({ negocio, motivoRechazo, resumen, metricas }) => (
        <li
          key={negocio.id}
          data-testid="mi-negocio"
          data-estado={negocio.estado}
          className="flex flex-col gap-4 rounded-card border border-border-base bg-surface p-5"
        >
          <div className="flex flex-wrap items-center gap-2">
            <strong className="text-fluid-lg text-text">{negocio.nombre}</strong>
            <span
              data-testid="estado-negocio"
              className="rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted"
            >
              {t(`estado.${negocio.estado}`)}
            </span>
          </div>

          {negocio.estado === "PENDIENTE" && (
            <p className="text-fluid-sm text-text-muted">{t("enRevision")}</p>
          )}

          {negocio.estado === "RECHAZADO" && motivoRechazo && (
            <p data-testid="motivo-rechazo" className="rounded-card bg-danger-subtle p-3 text-fluid-sm text-text">
              {t("motivoDelRechazo", { motivo: motivoRechazo })}
            </p>
          )}

          {/* Metricas: solo agregados, nunca quien. */}
          <dl className="grid grid-cols-3 gap-3" data-testid="metricas-negocio">
            <div className="rounded-card bg-surface-muted p-3">
              <dt className="text-fluid-sm text-text-muted">{t("visitas")}</dt>
              <dd className="text-fluid-xl font-bold text-text">{resumen.visitas}</dd>
            </div>
            <div className="rounded-card bg-surface-muted p-3">
              <dt className="text-fluid-sm text-text-muted">{t("whatsapp")}</dt>
              <dd className="text-fluid-xl font-bold text-text">{resumen.clicsWhatsapp}</dd>
            </div>
            <div className="rounded-card bg-surface-muted p-3">
              <dt className="text-fluid-sm text-text-muted">{t("comoLlegar")}</dt>
              <dd className="text-fluid-xl font-bold text-text">{resumen.clicsComoLlegar}</dd>
            </div>
          </dl>

          <p className="text-fluid-sm text-text-muted">
            {metricas.length === 0
              ? t("sinMetricasAun")
              : t("metricasDeDias", { dias: metricas.length })}
          </p>

          {negocio.estado === "APROBADO" && (
            <Link
              href={`/negocios/${negocio.id}`}
              className="press min-h-touch w-fit rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
            >
              {t("verFichaPublica")}
            </Link>
          )}
        </li>
      ))}
    </ul>
  );
}
