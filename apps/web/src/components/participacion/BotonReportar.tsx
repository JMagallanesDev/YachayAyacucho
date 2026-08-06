"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import { ErrorApi } from "@/lib/auth";
import { reportarContenido } from "@/lib/participacion";
import { useSesion } from "@/stores/sesion";
import type { MotivoReporte } from "@/types/participacion";

const MOTIVOS: MotivoReporte[] = [
  "OFENSIVO",
  "SPAM",
  "FALSO",
  "IRRELEVANTE",
  "DERECHOS_AUTOR",
  "OTRO",
];

/**
 * Reportar contenido inapropiado (RF-45).
 *
 * <p>Discreto a proposito: es un enlace pequeno, no un boton llamativo. Un
 * "reportar" prominente junto a cada opinion invita a usarlo como boton de "no
 * estoy de acuerdo", y tres reportes retiran el contenido de la vista publica.</p>
 */
export function BotonReportar({
  resenaId,
  fotoId,
}: {
  resenaId?: string;
  fotoId?: string;
}) {
  const t = useTranslations("participacion");
  const usuario = useSesion((estado) => estado.usuario);

  const [abierto, setAbierto] = useState(false);
  const [motivo, setMotivo] = useState<MotivoReporte>("OFENSIVO");
  const [enviando, setEnviando] = useState(false);
  const [hecho, setHecho] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!usuario) {
    return null;
  }

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setError(null);

    try {
      const resultado = await reportarContenido({ resenaId, fotoId, motivo });
      setHecho(resultado.enRevision ? t("reporteEnRevision") : t("reporteRecibido"));
      setAbierto(false);
    } catch (fallo) {
      setError(
        fallo instanceof ErrorApi
          ? (fallo.problema?.detail ?? t("errorReporte"))
          : t("errorReporte"),
      );
    } finally {
      setEnviando(false);
    }
  }

  if (hecho) {
    return (
      <p role="status" data-testid="reporte-hecho" className="text-fluid-sm text-text-muted">
        {hecho}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      {!abierto ? (
        <button
          type="button"
          onClick={() => setAbierto(true)}
          data-testid="abrir-reporte"
          className="w-fit text-fluid-sm text-text-muted underline-offset-4 hover:underline"
        >
          {t("reportar")}
        </button>
      ) : (
        <form onSubmit={enviar} className="flex flex-wrap items-center gap-2" data-testid="formulario-reporte">
          <label className="sr-only" htmlFor={`motivo-${resenaId ?? fotoId}`}>
            {t("motivoReporte")}
          </label>
          <select
            id={`motivo-${resenaId ?? fotoId}`}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value as MotivoReporte)}
            data-testid="motivo-reporte"
            className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-fluid-sm text-text"
          >
            {MOTIVOS.map((m) => (
              <option key={m} value={m}>
                {t(`motivo.${m}`)}
              </option>
            ))}
          </select>

          <button
            type="submit"
            disabled={enviando}
            data-testid="enviar-reporte"
            className="press min-h-touch rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text disabled:opacity-60"
          >
            {enviando ? t("enviando") : t("enviarReporte")}
          </button>
          <button
            type="button"
            onClick={() => setAbierto(false)}
            className="press min-h-touch px-2 text-fluid-sm text-text-muted"
          >
            {t("cancelar")}
          </button>
        </form>
      )}

      {error && (
        <p role="alert" data-testid="error-reporte" className="text-fluid-sm text-text">
          {error}
        </p>
      )}
    </div>
  );
}
