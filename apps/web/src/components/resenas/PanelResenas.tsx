"use client";

import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { BotonReportar } from "@/components/participacion/BotonReportar";
import { Estrellas } from "@/components/resenas/Estrellas";
import { ErrorApi } from "@/lib/auth";
import {
  borrarResena,
  crearResena,
  editarResena,
  listarResenas,
  miResena,
} from "@/lib/resenas";
import { Link } from "@/i18n/navegacion";
import { useSesion } from "@/stores/sesion";
import type { Resena } from "@/types/resena";

/**
 * Reseñas de un lugar (RF-37).
 *
 * <p>La lista se lee <strong>en vivo</strong> de la tabla, no de la vista
 * materializada: por eso una reseña recien escrita aparece al instante aunque
 * el promedio tarde hasta 30 s en recalcularse. Es una diferencia real y la
 * interfaz no la disimula.</p>
 */
export function PanelResenas({
  slug,
  resenasIniciales,
}: {
  slug: string;
  resenasIniciales: Resena[];
}) {
  const t = useTranslations("resenas");
  const usuario = useSesion((estado) => estado.usuario);

  const [resenas, setResenas] = useState<Resena[]>(resenasIniciales);
  const [mia, setMia] = useState<Resena | null>(null);
  const [calificacion, setCalificacion] = useState(5);
  const [comentario, setComentario] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editando, setEditando] = useState(false);

  // Al iniciar sesion se comprueba si ya habia opinado, para ofrecer editar en
  // lugar de crear. Sin esto, el formulario invitaria a escribir algo que el
  // servidor rechazaria con un 409.
  useEffect(() => {
    if (!usuario) {
      return;
    }
    void miResena(slug).then((encontrada) => {
      setMia(encontrada);
      if (encontrada) {
        setCalificacion(encontrada.calificacion);
        setComentario(encontrada.comentario ?? "");
      }
    });
  }, [usuario, slug]);

  async function recargar() {
    const pagina = await listarResenas(slug, "navegador");
    setResenas(pagina.content);
  }

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setError(null);

    try {
      const datos = { calificacion, comentario: comentario.trim() || undefined };
      const guardada = mia
        ? await editarResena(slug, mia.id, datos)
        : await crearResena(slug, datos);

      setMia(guardada);
      setEditando(false);
      await recargar();
    } catch (fallo) {
      setError(
        fallo instanceof ErrorApi
          ? (fallo.problema?.detail ?? t("errorGenerico"))
          : t("errorGenerico"),
      );
    } finally {
      setEnviando(false);
    }
  }

  async function borrar() {
    if (!mia) return;
    setEnviando(true);
    try {
      await borrarResena(slug, mia.id);
      setMia(null);
      setCalificacion(5);
      setComentario("");
      await recargar();
    } catch {
      setError(t("errorGenerico"));
    } finally {
      setEnviando(false);
    }
  }

  const mostrarFormulario = usuario && (!mia || editando);

  return (
    <section className="flex flex-col gap-5" data-testid="panel-resenas">
      <h2 className="text-fluid-xl font-semibold text-text">
        {t("titulo", { total: resenas.length })}
      </h2>

      {/* ---- Escribir o editar ---------------------------------------- */}
      {!usuario && (
        <p className="rounded-card bg-surface-muted p-4 text-fluid-sm text-text-muted">
          {t.rich("necesitasCuenta", {
            enlace: (texto) => (
              <Link href="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                {texto}
              </Link>
            ),
          })}
        </p>
      )}

      {usuario && mia && !editando && (
        <div
          className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-4"
          data-testid="mi-resena"
        >
          <p className="text-fluid-sm text-text-muted">{t("yaOpinaste")}</p>
          <Estrellas valor={mia.calificacion} />
          {mia.comentario && <p className="text-fluid-base text-text">{mia.comentario}</p>}
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setEditando(true)}
              data-testid="editar-resena"
              className="press min-h-touch rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
            >
              {t("editar")}
            </button>
            <button
              type="button"
              onClick={borrar}
              disabled={enviando}
              className="press min-h-touch rounded-card px-4 text-fluid-sm font-medium text-text-muted"
            >
              {t("borrar")}
            </button>
          </div>
        </div>
      )}

      {mostrarFormulario && (
        <form
          onSubmit={enviar}
          className="flex flex-col gap-4 rounded-card border border-border-base bg-surface p-4"
          data-testid="formulario-resena"
        >
          <Estrellas valor={calificacion} alCambiar={setCalificacion} />

          <label className="flex flex-col gap-1.5">
            <span className="text-fluid-sm text-text-muted">{t("comentario")}</span>
            <textarea
              value={comentario}
              onChange={(e) => setComentario(e.target.value)}
              maxLength={500}
              rows={3}
              placeholder={t("comentarioPlaceholder")}
              data-testid="comentario-resena"
              className="rounded-card border border-border-base bg-surface p-3 text-text"
            />
            <span className="self-end text-fluid-sm text-text-muted">
              {t("restantes", { restantes: 500 - comentario.length })}
            </span>
          </label>

          {error && (
            <p role="alert" className="rounded-card bg-danger-subtle p-3 text-fluid-sm text-text">
              {error}
            </p>
          )}

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={enviando}
              data-testid="enviar-resena"
              className="press min-h-touch rounded-card bg-primary px-5 text-fluid-sm font-medium text-primary-fg disabled:opacity-60"
            >
              {enviando ? t("enviando") : mia ? t("guardarCambios") : t("publicar")}
            </button>
            {editando && (
              <button
                type="button"
                onClick={() => setEditando(false)}
                className="press min-h-touch rounded-card px-4 text-fluid-sm text-text-muted"
              >
                {t("cancelar")}
              </button>
            )}
          </div>
        </form>
      )}

      {/* ---- Lista ------------------------------------------------------ */}
      {resenas.length === 0 ? (
        <p data-testid="sin-resenas" className="text-fluid-sm text-text-muted">
          {t("sinResenas")}
        </p>
      ) : (
        <ul className="flex flex-col gap-4" data-testid="lista-resenas">
          {resenas.map((resena) => (
            <li
              key={resena.id}
              data-testid="resena"
              // La nota se expone como atributo porque el texto de las
              // estrellas no sirve para leerla: se pintan siempre las cinco
              // (unas doradas y otras grises) y textContent las junta todas.
              data-calificacion={resena.calificacion}
              data-editada={resena.editada}
              className="flex flex-col gap-2 border-b border-border-base pb-4 last:border-0"
            >
              <div className="flex flex-wrap items-center gap-2">
                <Estrellas valor={resena.calificacion} tamano="sm" />
                <span className="text-fluid-sm font-medium text-text">{resena.autor}</span>
                {resena.editada && (
                  <span className="text-fluid-sm text-text-muted">{t("editada")}</span>
                )}
              </div>
              {resena.comentario && (
                <p className="text-fluid-base text-text">{resena.comentario}</p>
              )}
              {/* Solo en las ajenas: reportar la propia no tiene sentido y el
                  servidor lo rechaza igualmente. */}
              {resena.autorId !== usuario?.id && <BotonReportar resenaId={resena.id} />}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
