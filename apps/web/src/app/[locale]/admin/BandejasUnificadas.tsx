"use client";

import { useLocale, useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { ErrorApi } from "@/lib/auth";
import { metricas } from "@/lib/admin";
import type { Pendientes } from "@/types/admin";

import { Moderacion } from "./Moderacion";
import { ModeracionNegocios } from "./ModeracionNegocios";
import { ModeracionReportes } from "./ModeracionReportes";

type Pestana = "fotos" | "resenas" | "reportes" | "negocios";

/**
 * Las tres bandejas de moderacion, en un solo sitio (Bloque 10).
 *
 * <p><strong>Por que se unifican.</strong> Estaban dispersas: fotos y resenas
 * en un componente y los reportes ciudadanos en otro, cada una pidiendo lo suyo
 * y sin decir cuanto quedaba por revisar. Quien modera tenia que recorrer la
 * pagina entera para saber si habia trabajo. Ahora hay un contador por pestana
 * en una sola llamada, asi que de un vistazo se ve donde hace falta entrar.</p>
 *
 * <p>Las pestanas <strong>montan solo la bandeja activa</strong>: cada una hace
 * su propia peticion al abrirse, y cargar las tres de golpe seria pedir datos
 * que probablemente nadie va a mirar.</p>
 */
export function BandejasUnificadas() {
  const t = useTranslations("panel");
  const idioma = useLocale();
  const { comprobando } = useSesionRequerida();

  const [pendientes, setPendientes] = useState<Pendientes | null>(null);
  const [sinPermisos, setSinPermisos] = useState(false);
  const [activa, setActiva] = useState<Pestana>("fotos");

  const cargar = useCallback(() => metricas(idioma), [idioma]);

  useEffect(() => {
    if (comprobando) {
      return;
    }
    cargar()
      .then((datos) => setPendientes(datos.pendientes))
      .catch((fallo) => {
        if (fallo instanceof ErrorApi && fallo.estado === 403) {
          setSinPermisos(true);
        }
      });
  }, [comprobando, cargar]);

  if (comprobando || sinPermisos) {
    return null;
  }

  const pestanas: { clave: Pestana; cuenta: number }[] = [
    { clave: "fotos", cuenta: pendientes?.fotos ?? 0 },
    { clave: "resenas", cuenta: pendientes?.resenas ?? 0 },
    { clave: "reportes", cuenta: pendientes?.reportes ?? 0 },
    // Los negocios pendientes llegan en el mismo resumen del panel desde el
    // Bloque 11: una sola llamada sigue bastando para los cuatro contadores.
    { clave: "negocios", cuenta: pendientes?.negocios ?? 0 },
  ];

  const total = pestanas.reduce((suma, pestana) => suma + pestana.cuenta, 0);

  return (
    <section className="flex flex-col gap-4" data-testid="bandejas-unificadas">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-fluid-xl font-semibold text-text">{t("moderacion")}</h2>
        <span
          data-testid="pendientes-total"
          data-total={total}
          className="text-fluid-sm text-text-muted"
        >
          {t("pendientesTotal", { total })}
        </span>
      </div>

      <div role="tablist" aria-label={t("moderacion")} className="flex flex-wrap gap-2">
        {pestanas.map((pestana) => (
          <button
            key={pestana.clave}
            type="button"
            role="tab"
            aria-selected={activa === pestana.clave}
            onClick={() => setActiva(pestana.clave)}
            data-testid={`pestana-${pestana.clave}`}
            data-cuenta={pestana.cuenta}
            className={`press min-h-touch rounded-card px-4 text-fluid-sm font-medium ${
              activa === pestana.clave
                ? "bg-primary text-primary-fg"
                : "bg-surface-muted text-text-muted"
            }`}
          >
            {t(`bandeja.${pestana.clave}`)}
            {pestana.cuenta > 0 && (
              <span
                className={`ms-2 rounded-full px-2 py-0.5 text-xs tabular-nums ${
                  activa === pestana.clave ? "bg-primary-fg/20" : "bg-surface"
                }`}
              >
                {pestana.cuenta}
              </span>
            )}
          </button>
        ))}
      </div>

      <div role="tabpanel" data-testid={`panel-${activa}`}>
        {/* `Moderacion` trae las dos primeras bandejas desde el Bloque 6; se
            le dice cual mostrar en vez de duplicar su logica aqui. */}
        {activa === "reportes" ? (
          <ModeracionReportes />
        ) : activa === "negocios" ? (
          <ModeracionNegocios />
        ) : (
          <Moderacion solo={activa} />
        )}
      </div>
    </section>
  );
}
