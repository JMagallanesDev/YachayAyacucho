"use client";

import { useLocale, useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { ErrorApi } from "@/lib/auth";
import { bandejaReportes, cambiarEstadoReporte } from "@/lib/reportes";
import type { EstadoReporte, Reporte } from "@/types/reporte";
import Image from "next/image";
import { TAMANOS, cargadorCloudinary } from "@/lib/imagenes";

const ESTADOS: EstadoReporte[] = [
  "RECIBIDO",
  "EN_REVISION",
  "APROBADO",
  "DESCARTADO",
  "RESUELTO",
];

/**
 * Bandeja de reportes ciudadanos (RF-76).
 *
 * <p>Es el filtro que separa una denuncia util de una difamacion: nada llega al
 * mapa publico sin pasar por aqui. Las <strong>notas internas</strong> quedan
 * solo en esta pantalla; el API no las devuelve en ninguna respuesta publica.</p>
 */
export function ModeracionReportes() {
  const t = useTranslations("moderacionReportes");
  const idioma = useLocale();
  const { comprobando } = useSesionRequerida();

  const [reportes, setReportes] = useState<Reporte[]>([]);
  const [sinPermisos, setSinPermisos] = useState(false);
  const [notas, setNotas] = useState<Record<string, string>>({});
  const [trabajando, setTrabajando] = useState<string | null>(null);

  const cargar = useCallback(() => bandejaReportes(idioma), [idioma]);

  const aplicar = useCallback((lista: Reporte[]) => {
    setReportes(lista);
    setSinPermisos(false);
  }, []);

  const alFallar = useCallback((fallo: unknown) => {
    if (fallo instanceof ErrorApi && fallo.estado === 403) {
      setSinPermisos(true);
    }
  }, []);

  useEffect(() => {
    if (!comprobando) {
      cargar().then(aplicar).catch(alFallar);
    }
  }, [comprobando, cargar, aplicar, alFallar]);

  async function cambiar(reporte: Reporte, estado: EstadoReporte) {
    setTrabajando(reporte.id);
    try {
      await cambiarEstadoReporte(reporte.id, estado, notas[reporte.id]);
      aplicar(await cargar());
    } catch (fallo) {
      alFallar(fallo);
    } finally {
      setTrabajando(null);
    }
  }

  if (comprobando || sinPermisos) {
    return null;
  }

  return (
    <section className="flex flex-col gap-4" data-testid="moderacion-reportes">
      <h2 className="text-fluid-xl font-semibold text-text">
        {t("titulo", { total: reportes.length })}
      </h2>

      {reportes.length === 0 ? (
        <p data-testid="sin-reportes" className="text-fluid-sm text-text-muted">
          {t("sinReportes")}
        </p>
      ) : (
        <ul className="flex flex-col gap-4" data-testid="bandeja-reportes">
          {reportes.map((reporte) => (
            <li
              key={reporte.id}
              data-testid="reporte-moderable"
              data-estado={reporte.estado}
              data-anonimo={reporte.esAnonimo}
              className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-4"
            >
              <div className="flex flex-wrap items-center gap-2">
                <span
                  className="rounded-full px-2.5 py-1 text-fluid-sm font-medium text-text"
                  style={{ backgroundColor: `${reporte.colorHex}2e` }}
                >
                  {reporte.tipoNombre}
                </span>
                <span className="rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted">
                  {t(`estado.${reporte.estado}`)}
                </span>
                {reporte.esAnonimo && (
                  <span
                    data-testid="marca-anonimo"
                    className="rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted"
                  >
                    {t("anonimo")}
                  </span>
                )}
              </div>

              <p className="text-fluid-base text-text">{reporte.descripcion}</p>

              {reporte.direccionReferencial && (
                <p className="text-fluid-sm text-text-muted">{reporte.direccionReferencial}</p>
              )}

              {reporte.fotos.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {reporte.fotos.map((url) => (
                    <Image
                      key={url}
                      src={url}
                      alt=""
                      width={112}
                      height={112}
                      sizes={TAMANOS.miniatura}
                      loader={cargadorCloudinary}
                      className="size-28 rounded-card object-cover"
                    />
                  ))}
                </div>
              )}

              <label className="flex flex-col gap-1">
                <span className="text-fluid-sm text-text-muted">{t("notasInternas")}</span>
                <textarea
                  value={notas[reporte.id] ?? reporte.notasAdmin ?? ""}
                  onChange={(e) => setNotas({ ...notas, [reporte.id]: e.target.value })}
                  rows={2}
                  maxLength={2000}
                  data-testid="notas-reporte"
                  className="rounded-card border border-border-base bg-surface p-2 text-fluid-sm text-text"
                />
              </label>

              <div className="flex flex-wrap gap-2">
                {ESTADOS.filter((estado) => estado !== reporte.estado).map((estado) => (
                  <button
                    key={estado}
                    type="button"
                    disabled={trabajando === reporte.id}
                    onClick={() => cambiar(reporte, estado)}
                    data-testid={`estado-${estado}`}
                    className="press min-h-touch rounded-card border border-border-strong px-3 text-fluid-sm font-medium text-text disabled:opacity-60"
                  >
                    {t(`estado.${estado}`)}
                  </button>
                ))}
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
