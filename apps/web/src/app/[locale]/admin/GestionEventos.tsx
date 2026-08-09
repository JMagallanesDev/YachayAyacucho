"use client";

import { useLocale, useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { ErrorApi } from "@/lib/auth";
import { env } from "@/lib/env";
import {
  actualizarEvento,
  bandejaEventos,
  clonarEvento,
  crearEvento,
  eliminarEvento,
} from "@/lib/eventos";
import { formatearRango, hoyEnAyacucho } from "@/lib/fechas";
import type { Evento, EstadoEvento, NuevoEvento, TipoEvento } from "@/types/evento";

const TIPOS: TipoEvento[] = [
  "RELIGIOSO",
  "CIVICO",
  "CULTURAL",
  "GASTRONOMICO",
  "ARTESANAL",
  "MUSICAL",
  "OTRO",
];

const ESTADOS: EstadoEvento[] = ["BORRADOR", "PUBLICADO", "CANCELADO", "ARCHIVADO"];

interface Distrito {
  id: string;
  nombre: string;
  provincia: string;
}

/**
 * Gestion de la agenda cultural (RF-86).
 *
 * <p>Lo unico que tiene de particular frente a un CRUD normal es el
 * <strong>clonado anual</strong>: para una festividad recurrente aparece un
 * campo de anio y un boton que crea la edicion de ese anio como borrador. El
 * clon no copia la fecha vieja, y el aviso junto al boton lo dice, porque un
 * administrador que no lo sepa podria publicarlo sin revisar y anunciar una
 * Semana Santa en la fecha equivocada.</p>
 */
export function GestionEventos() {
  const t = useTranslations("agendaAdmin");
  const idioma = useLocale();
  const { comprobando } = useSesionRequerida();

  const [eventos, setEventos] = useState<Evento[]>([]);
  const [distritos, setDistritos] = useState<Distrito[]>([]);
  const [sinPermisos, setSinPermisos] = useState(false);
  const [trabajando, setTrabajando] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editando, setEditando] = useState<string | null>(null);
  const [anios, setAnios] = useState<Record<string, string>>({});

  const hoy = hoyEnAyacucho();
  const [formulario, setFormulario] = useState<NuevoEvento>({
    distritoId: "",
    tipo: "CULTURAL",
    fechaInicio: hoy,
    fechaFin: hoy,
    recurrenteAnual: false,
    estado: "BORRADOR",
    traducciones: [{ idioma: "ES", nombre: "", descripcion: "", organizador: "" }],
  });

  const cargar = useCallback(() => bandejaEventos(idioma), [idioma]);

  const alFallar = useCallback((fallo: unknown) => {
    if (fallo instanceof ErrorApi && fallo.estado === 403) {
      setSinPermisos(true);
      return;
    }
    setError(fallo instanceof Error ? fallo.message : "Error");
  }, []);

  useEffect(() => {
    if (comprobando) {
      return;
    }
    cargar().then(setEventos).catch(alFallar);

    fetch(`${env.apiUrl}/distritos`)
      .then((r) => (r.ok ? r.json() : []))
      .then(setDistritos)
      .catch(() => setDistritos([]));
  }, [comprobando, cargar, alFallar]);

  async function guardar(evento: React.FormEvent) {
    evento.preventDefault();
    setError(null);
    setTrabajando("formulario");
    try {
      if (editando) {
        await actualizarEvento(editando, formulario, idioma);
      } else {
        await crearEvento(formulario, idioma);
      }
      setEditando(null);
      setEventos(await cargar());
    } catch (fallo) {
      alFallar(fallo);
    } finally {
      setTrabajando(null);
    }
  }

  async function clonar(evento: Evento) {
    const anio = Number(anios[evento.id]);
    if (!anio) {
      return;
    }
    setError(null);
    setTrabajando(evento.id);
    try {
      await clonarEvento(evento.id, anio, idioma);
      setEventos(await cargar());
    } catch (fallo) {
      alFallar(fallo);
    } finally {
      setTrabajando(null);
    }
  }

  async function borrar(evento: Evento) {
    setTrabajando(evento.id);
    try {
      await eliminarEvento(evento.id);
      setEventos(await cargar());
    } catch (fallo) {
      alFallar(fallo);
    } finally {
      setTrabajando(null);
    }
  }

  function editar(evento: Evento) {
    setEditando(evento.id);
    setFormulario({
      distritoId: distritos.find((d) => d.nombre === evento.distritoNombre)?.id ?? "",
      tipo: evento.tipo,
      fechaInicio: evento.fechaInicio,
      fechaFin: evento.fechaFin,
      recurrenteAnual: evento.recurrenteAnual,
      estado: evento.estado,
      traducciones: [
        {
          idioma: "ES",
          nombre: evento.nombre,
          descripcion: evento.descripcion ?? "",
          organizador: evento.organizador ?? "",
        },
      ],
    });
  }

  if (comprobando || sinPermisos) {
    return null;
  }

  return (
    <section className="flex flex-col gap-5" data-testid="gestion-eventos">
      <h2 className="text-fluid-xl font-semibold text-text">
        {t("titulo", { total: eventos.length })}
      </h2>

      {error && (
        <p role="alert" data-testid="error-evento" className="rounded-card bg-danger-subtle p-4 text-text">
          {error}
        </p>
      )}

      {/* ---- Alta y edicion ------------------------------------------- */}
      <form
        onSubmit={guardar}
        data-testid="formulario-evento"
        className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-4"
      >
        <h3 className="text-fluid-base font-semibold text-text">
          {editando ? t("editando") : t("nuevoEvento")}
        </h3>

        <label className="flex flex-col gap-1">
          <span className="text-fluid-sm text-text-muted">{t("nombre")}</span>
          <input
            type="text"
            required
            maxLength={200}
            value={formulario.traducciones[0].nombre}
            onChange={(e) =>
              setFormulario({
                ...formulario,
                traducciones: [{ ...formulario.traducciones[0], nombre: e.target.value }],
              })
            }
            data-testid="evento-nombre"
            className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-fluid-sm text-text-muted">{t("descripcion")}</span>
          <textarea
            rows={2}
            value={formulario.traducciones[0].descripcion}
            onChange={(e) =>
              setFormulario({
                ...formulario,
                traducciones: [{ ...formulario.traducciones[0], descripcion: e.target.value }],
              })
            }
            data-testid="evento-descripcion"
            className="rounded-card border border-border-base bg-surface p-2 text-text"
          />
        </label>

        <div className="flex flex-wrap gap-3">
          <label className="flex flex-col gap-1">
            <span className="text-fluid-sm text-text-muted">{t("fechaInicio")}</span>
            <input
              type="date"
              required
              value={formulario.fechaInicio}
              onChange={(e) =>
                setFormulario({
                  ...formulario,
                  fechaInicio: e.target.value,
                  // La fecha de fin sigue a la de inicio mientras no la muevan
                  // a mano: casi todos los eventos duran un dia.
                  fechaFin:
                    formulario.fechaFin < e.target.value ? e.target.value : formulario.fechaFin,
                })
              }
              data-testid="evento-inicio"
              className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
            />
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-fluid-sm text-text-muted">{t("fechaFin")}</span>
            <input
              type="date"
              required
              min={formulario.fechaInicio}
              value={formulario.fechaFin}
              onChange={(e) => setFormulario({ ...formulario, fechaFin: e.target.value })}
              data-testid="evento-fin"
              className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
            />
          </label>
        </div>

        <div className="flex flex-wrap gap-3">
          <label className="flex flex-col gap-1">
            {/* Ojo: `tipo` y `estado` son mapas anidados (tipo.RELIGIOSO...),
                asi que las etiquetas tienen su propia clave. Pedir el mapa como
                si fuera texto rompe el componente entero. */}
            <span className="text-fluid-sm text-text-muted">{t("etiquetaTipo")}</span>
            <select
              value={formulario.tipo}
              onChange={(e) => setFormulario({ ...formulario, tipo: e.target.value as TipoEvento })}
              data-testid="evento-tipo"
              className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
            >
              {TIPOS.map((tipo) => (
                <option key={tipo} value={tipo}>
                  {t(`tipo.${tipo}`)}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-fluid-sm text-text-muted">{t("distrito")}</span>
            <select
              required
              value={formulario.distritoId}
              onChange={(e) => setFormulario({ ...formulario, distritoId: e.target.value })}
              data-testid="evento-distrito"
              className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
            >
              <option value="">{t("elegirDistrito")}</option>
              {distritos.map((distrito) => (
                <option key={distrito.id} value={distrito.id}>
                  {distrito.nombre} ({distrito.provincia})
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-fluid-sm text-text-muted">{t("etiquetaEstado")}</span>
            <select
              value={formulario.estado}
              onChange={(e) =>
                setFormulario({ ...formulario, estado: e.target.value as EstadoEvento })
              }
              data-testid="evento-estado"
              className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
            >
              {ESTADOS.map((estado) => (
                <option key={estado} value={estado}>
                  {t(`estado.${estado}`)}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label className="flex items-start gap-3">
          <input
            type="checkbox"
            checked={formulario.recurrenteAnual}
            onChange={(e) => setFormulario({ ...formulario, recurrenteAnual: e.target.checked })}
            data-testid="evento-recurrente"
            className="mt-1 size-5"
          />
          <span className="flex flex-col gap-1">
            <span className="text-fluid-base text-text">{t("recurrente")}</span>
            <span className="text-fluid-sm text-text-muted">{t("recurrenteAyuda")}</span>
          </span>
        </label>

        <div className="flex flex-wrap gap-2">
          <button
            type="submit"
            disabled={trabajando === "formulario"}
            data-testid="guardar-evento"
            className="press min-h-touch rounded-card bg-primary px-5 py-2 text-fluid-sm font-medium text-primary-fg disabled:opacity-50"
          >
            {editando ? t("guardarCambios") : t("crear")}
          </button>
          {editando && (
            <button
              type="button"
              onClick={() => setEditando(null)}
              className="press min-h-touch rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
            >
              {t("cancelar")}
            </button>
          )}
        </div>
      </form>

      {/* ---- Bandeja --------------------------------------------------- */}
      <ul className="flex flex-col gap-3" data-testid="bandeja-eventos">
        {eventos.map((evento) => (
          <li
            key={evento.id}
            data-testid="evento-gestionable"
            data-estado={evento.estado}
            data-recurrente={evento.recurrenteAnual}
            className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-4"
          >
            <div className="flex flex-wrap items-center gap-2">
              <strong className="text-fluid-base text-text">{evento.nombre}</strong>
              <span className="rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted">
                {t(`estado.${evento.estado}`)}
              </span>
            </div>

            <span className="text-fluid-sm text-text-muted">
              {formatearRango(evento.fechaInicio, evento.fechaFin, idioma)}
            </span>

            <div className="flex flex-wrap items-end gap-2">
              <button
                type="button"
                onClick={() => editar(evento)}
                data-testid="editar-evento"
                className="press min-h-touch rounded-card border border-border-strong px-3 text-fluid-sm font-medium text-text"
              >
                {t("editar")}
              </button>

              <button
                type="button"
                onClick={() => borrar(evento)}
                disabled={trabajando === evento.id}
                data-testid="borrar-evento"
                className="press min-h-touch rounded-card border border-border-strong px-3 text-fluid-sm font-medium text-text disabled:opacity-60"
              >
                {t("darDeBaja")}
              </button>

              {evento.recurrenteAnual && (
                <div className="flex items-end gap-2">
                  <label className="flex flex-col gap-1">
                    <span className="text-fluid-sm text-text-muted">{t("clonarA")}</span>
                    <input
                      type="number"
                      min={2000}
                      max={2100}
                      placeholder={String(Number(hoy.slice(0, 4)) + 1)}
                      value={anios[evento.id] ?? ""}
                      onChange={(e) => setAnios({ ...anios, [evento.id]: e.target.value })}
                      data-testid="anio-clon"
                      className="min-h-touch w-24 rounded-card border border-border-base bg-surface px-3 text-text"
                    />
                  </label>
                  <button
                    type="button"
                    onClick={() => clonar(evento)}
                    disabled={trabajando === evento.id}
                    data-testid="clonar-evento"
                    className="press min-h-touch rounded-card bg-accent px-3 text-fluid-sm font-medium text-accent-fg disabled:opacity-60"
                  >
                    {t("clonar")}
                  </button>
                </div>
              )}
            </div>

            {evento.recurrenteAnual && (
              <p className="text-fluid-sm text-text-muted" data-testid="aviso-clonado">
                {t("avisoClonado")}
              </p>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
