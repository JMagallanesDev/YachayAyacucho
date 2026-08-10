"use client";

import { useFormatter, useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { registroDeActividad } from "@/lib/admin";
import { ErrorApi } from "@/lib/auth";
import type { Actividad } from "@/types/admin";

/**
 * Bitacora de acciones administrativas (RF-56).
 *
 * <p>Se muestra la IP de quien actuo, y conviene saber por que no contradice el
 * anonimato del Bloque 8: alli se protege a un ciudadano que denuncia un dano al
 * patrimonio; aqui se registra a un administrador identificado que ejerce
 * privilegios sobre contenido ajeno. La fila ya lleva su nombre y su correo, de
 * modo que la IP no anade identificabilidad —eso ya estaba— sino desde donde se
 * ejercio ese poder.</p>
 */
export function RegistroActividad() {
  const t = useTranslations("panel");
  const formato = useFormatter();
  const { comprobando } = useSesionRequerida();

  const [lineas, setLineas] = useState<Actividad[]>([]);
  const [sinPermisos, setSinPermisos] = useState(false);

  useEffect(() => {
    if (comprobando) {
      return;
    }
    registroDeActividad(50)
      .then(setLineas)
      .catch((fallo) => {
        if (fallo instanceof ErrorApi && fallo.estado === 403) {
          setSinPermisos(true);
        }
      });
  }, [comprobando]);

  if (comprobando || sinPermisos) {
    return null;
  }

  return (
    <section className="flex flex-col gap-4" data-testid="registro-actividad">
      <div className="flex flex-col gap-1">
        <h2 className="text-fluid-xl font-semibold text-text">{t("actividad")}</h2>
        <p className="text-fluid-sm text-text-muted">{t("actividadAyuda")}</p>
      </div>

      {lineas.length === 0 ? (
        <p data-testid="sin-actividad" className="text-fluid-sm text-text-muted">
          {t("sinActividad")}
        </p>
      ) : (
        <ol className="flex flex-col gap-2" data-testid="lista-actividad">
          {lineas.map((linea) => (
            <li
              key={linea.id}
              data-testid="linea-actividad"
              data-accion={linea.accion}
              className="flex flex-wrap items-baseline gap-x-3 gap-y-1 rounded-card border border-border-base bg-surface px-4 py-3 text-fluid-sm"
            >
              <span className="font-medium text-text">{linea.autorNombre}</span>
              <span className="rounded-full bg-surface-muted px-2 py-0.5 font-medium text-text-muted">
                {linea.accion}
              </span>
              <span className="text-text-muted">{linea.entidad}</span>
              <span className="ms-auto tabular-nums text-text-muted">
                {formato.dateTime(new Date(linea.ocurridoEn), {
                  dateStyle: "short",
                  timeStyle: "short",
                })}
              </span>
              {linea.ip && (
                <span className="w-full font-mono text-xs text-text-muted">{linea.ip}</span>
              )}
              {linea.detalles && (
                <span className="w-full break-all font-mono text-xs text-text-muted">
                  {linea.detalles}
                </span>
              )}
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
