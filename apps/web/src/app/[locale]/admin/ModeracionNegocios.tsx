"use client";

import { useCallback, useEffect, useState } from "react";
import { useLocale, useTranslations } from "next-intl";

import { ErrorApi } from "@/lib/auth";
import { bandejaNegocios, cambiarEstadoNegocio } from "@/lib/negocios";
import type { EstadoNegocio, Negocio } from "@/types/negocio";

/**
 * Bandeja de aprobacion del directorio (RF-104).
 *
 * <p>Es la puerta que separa «alguien pidio aparecer» de «aparece». El motivo
 * que se escribe al rechazar <strong>lo lee despues su dueno</strong> en su
 * propio panel: se guarda en la bitacora del Bloque 10, asi que conviene que
 * diga algo util y no «no».</p>
 */
export function ModeracionNegocios() {
  const t = useTranslations("moderacionNegocios");
  const idioma = useLocale();

  const [negocios, setNegocios] = useState<Negocio[]>([]);
  const [motivos, setMotivos] = useState<Record<string, string>>({});
  const [trabajando, setTrabajando] = useState<string | null>(null);

  const cargar = useCallback(() => bandejaNegocios(idioma), [idioma]);

  useEffect(() => {
    cargar().then(setNegocios).catch(() => setNegocios([]));
  }, [cargar]);

  async function cambiar(negocio: Negocio, estado: EstadoNegocio) {
    setTrabajando(negocio.id);
    try {
      await cambiarEstadoNegocio(negocio.id, estado, motivos[negocio.id]);
      setNegocios(await cargar());
    } catch (fallo) {
      if (!(fallo instanceof ErrorApi)) {
        throw fallo;
      }
    } finally {
      setTrabajando(null);
    }
  }

  if (negocios.length === 0) {
    return (
      <p data-testid="sin-negocios-moderar" className="text-fluid-sm text-text-muted">
        {t("sinNegocios")}
      </p>
    );
  }

  return (
    <ul className="flex flex-col gap-4" data-testid="bandeja-negocios">
      {negocios.map((negocio) => (
        <li
          key={negocio.id}
          data-testid="negocio-moderable"
          data-estado={negocio.estado}
          className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-4"
        >
          <div className="flex flex-wrap items-center gap-2">
            <strong className="text-fluid-base text-text">{negocio.nombre}</strong>
            <span className="rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted">
              {t(`estado.${negocio.estado}`)}
            </span>
            <span className="rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted">
              {negocio.categoriaNombre}
            </span>
          </div>

          {negocio.descripcion && (
            <p className="text-fluid-sm text-text-muted">{negocio.descripcion}</p>
          )}

          <p className="text-fluid-sm text-text-muted">
            {[negocio.direccion, negocio.distritoNombre, negocio.telefono]
              .filter(Boolean)
              .join(" · ")}
          </p>

          <label className="flex flex-col gap-1">
            <span className="text-fluid-sm text-text-muted">{t("motivo")}</span>
            <input
              type="text"
              maxLength={500}
              placeholder={t("motivoAyuda")}
              value={motivos[negocio.id] ?? ""}
              onChange={(e) => setMotivos({ ...motivos, [negocio.id]: e.target.value })}
              data-testid="motivo-negocio"
              className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-fluid-sm text-text"
            />
          </label>

          <div className="flex flex-wrap gap-2">
            {(["APROBADO", "RECHAZADO", "SUSPENDIDO"] as EstadoNegocio[])
              .filter((estado) => estado !== negocio.estado)
              .map((estado) => (
                <button
                  key={estado}
                  type="button"
                  disabled={trabajando === negocio.id}
                  onClick={() => cambiar(negocio, estado)}
                  data-testid={`negocio-${estado}`}
                  className="press min-h-touch rounded-card border border-border-strong px-3 text-fluid-sm font-medium text-text disabled:opacity-60"
                >
                  {t(`accion.${estado}`)}
                </button>
              ))}
          </div>
        </li>
      ))}
    </ul>
  );
}
