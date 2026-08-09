"use client";

import { useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { Estrellas } from "@/components/resenas/Estrellas";
import { useSesionRequerida } from "@/components/useSesionRequerida";
import { ErrorApi, pedirAutenticado } from "@/lib/auth";
import type { FotoModeracion, ResenaModeracion } from "@/types/resena";

/**
 * Bandejas de moderacion (RF-49, RF-50).
 *
 * <p>Que esta pantalla se vea no autoriza nada: cada llamada la vuelve a
 * comprobar el backend con {@code @PreAuthorize}. Aqui se oculta la interfaz a
 * quien no es administrador solo por cortesia, nunca como medida de
 * seguridad.</p>
 */
export function Moderacion({ solo }: { solo?: "fotos" | "resenas" } = {}) {
  const t = useTranslations("moderacion");
  const { comprobando } = useSesionRequerida();

  const [fotos, setFotos] = useState<FotoModeracion[]>([]);
  const [resenas, setResenas] = useState<ResenaModeracion[]>([]);
  const [sinPermisos, setSinPermisos] = useState(false);
  const [trabajando, setTrabajando] = useState<string | null>(null);

  /**
   * Trae ambas bandejas.
   *
   * <p>Devuelve los datos en vez de fijar el estado, y quien la llama lo hace
   * en un `.then`. No es capricho: la regla `set-state-in-effect` no puede
   * saber que una funcion async no toca el estado antes del primer `await`, y
   * marcaria el efecto como cascada de renderizados.</p>
   */
  const cargar = useCallback(
    () =>
      Promise.all([
        pedirAutenticado<FotoModeracion[]>("/admin/moderacion/fotos"),
        pedirAutenticado<ResenaModeracion[]>("/admin/moderacion/resenas"),
      ]),
    [],
  );

  const aplicar = useCallback(
    ([f, r]: [FotoModeracion[], ResenaModeracion[]]) => {
      setFotos(f);
      setResenas(r);
      setSinPermisos(false);
    },
    [],
  );

  const alFallar = useCallback((fallo: unknown) => {
    // 403 es la respuesta correcta para una cuenta sin rol ADMIN: la
    // autorizacion la decide el backend, no esta pantalla.
    if (fallo instanceof ErrorApi && fallo.estado === 403) {
      setSinPermisos(true);
    }
  }, []);

  useEffect(() => {
    if (!comprobando) {
      cargar().then(aplicar).catch(alFallar);
    }
  }, [comprobando, cargar, aplicar, alFallar]);

  async function accion(ruta: string, id: string) {
    setTrabajando(id);
    try {
      await pedirAutenticado<void>(ruta, { method: "POST", body: JSON.stringify({}) });
      aplicar(await cargar());
    } catch (fallo) {
      alFallar(fallo);
    } finally {
      setTrabajando(null);
    }
  }

  if (comprobando) {
    return null;
  }

  if (sinPermisos) {
    return (
      <p role="alert" className="rounded-card bg-danger-subtle p-4 text-text">
        {t("sinPermisos")}
      </p>
    );
  }

  // `solo` lo pasa la bandeja unificada del Bloque 10 para mostrar una pestana
  // cada vez. Sin el, se pintan las dos, que es como se usaba hasta ahora.
  const muestraFotos = solo === undefined || solo === "fotos";
  const muestraResenas = solo === undefined || solo === "resenas";

  return (
    <div className="flex flex-col gap-8" data-testid="moderacion">
      {/* ---- Fotos pendientes (RF-49) --------------------------------- */}
      <section className="flex flex-col gap-3" hidden={!muestraFotos}>
        <h2 className="text-fluid-xl font-semibold text-text">
          {t("fotosPendientes", { total: fotos.length })}
        </h2>

        {fotos.length === 0 ? (
          <p data-testid="sin-fotos-pendientes" className="text-fluid-sm text-text-muted">
            {t("nadaPendiente")}
          </p>
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2" data-testid="bandeja-fotos">
            {fotos.map((foto) => (
              <li
                key={foto.id}
                data-testid="foto-pendiente"
                data-estado={foto.estado}
                className="flex flex-col gap-2 rounded-card border border-border-base bg-surface p-3"
              >
                {/* Una foto EN_REVISION llego aqui porque tres personas la
                    denunciaron (RF-45), no porque acabe de subirse. Merece
                    mirarse antes y con otro criterio, asi que se marca. Antes
                    del Bloque 10 ni siquiera entraba en esta cola. */}
                {foto.estado === "EN_REVISION" && (
                  <span
                    data-testid="foto-denunciada"
                    className="w-fit rounded-full bg-danger-subtle px-2.5 py-1 text-fluid-sm font-medium text-text"
                  >
                    {t("denunciada")}
                  </span>
                )}

                {/* Sin transformar: quien modera necesita ver la imagen tal
                    cual se subio, no una version recortada. */}
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={foto.url}
                  alt=""
                  className="h-48 w-full rounded-card object-cover"
                />
                <p className="text-fluid-sm text-text-muted">
                  {t("subidaPor", { autor: foto.autor, lugar: foto.lugarSlug })}
                </p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    disabled={trabajando === foto.id}
                    onClick={() => accion(`/admin/moderacion/fotos/${foto.id}/aprobar`, foto.id)}
                    data-testid="aprobar-foto"
                    className="press min-h-touch rounded-card bg-primary px-4 text-fluid-sm font-medium text-primary-fg disabled:opacity-60"
                  >
                    {t("aprobar")}
                  </button>
                  <button
                    type="button"
                    disabled={trabajando === foto.id}
                    onClick={() => accion(`/admin/moderacion/fotos/${foto.id}/rechazar`, foto.id)}
                    data-testid="rechazar-foto"
                    className="press min-h-touch rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text disabled:opacity-60"
                  >
                    {t("rechazar")}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* ---- Reseñas (RF-50) ------------------------------------------ */}
      <section className="flex flex-col gap-3" hidden={!muestraResenas}>
        <h2 className="text-fluid-xl font-semibold text-text">{t("resenas")}</h2>

        {resenas.length === 0 ? (
          <p className="text-fluid-sm text-text-muted">{t("sinResenas")}</p>
        ) : (
          <ul className="flex flex-col gap-3" data-testid="bandeja-resenas">
            {resenas.map((resena) => (
              <li
                key={resena.id}
                data-testid="resena-moderable"
                data-estado={resena.estado}
                className="flex flex-col gap-2 rounded-card border border-border-base bg-surface p-3"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <Estrellas valor={resena.calificacion} tamano="sm" />
                  <span className="text-fluid-sm font-medium text-text">{resena.autor}</span>
                  <span className="text-fluid-sm text-text-muted">{resena.lugarSlug}</span>
                  {/* La marca de editada es lo que cierra el hueco de
                      "escribo algo correcto y luego lo cambio". */}
                  {resena.editada && (
                    <span
                      data-testid="marca-editada"
                      className="rounded-full bg-accent-subtle px-2 py-0.5 text-fluid-sm font-medium text-text"
                    >
                      {t("editada")}
                    </span>
                  )}
                  {resena.estado === "OCULTA" && (
                    <span className="rounded-full bg-surface-muted px-2 py-0.5 text-fluid-sm text-text-muted">
                      {t("oculta")}
                    </span>
                  )}
                </div>

                {resena.comentario && (
                  <p className="text-fluid-base text-text">{resena.comentario}</p>
                )}

                {resena.estado === "OCULTA" ? (
                  <button
                    type="button"
                    disabled={trabajando === resena.id}
                    onClick={() =>
                      accion(`/admin/moderacion/resenas/${resena.id}/restaurar`, resena.id)
                    }
                    data-testid="restaurar-resena"
                    className="press min-h-touch w-fit rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text disabled:opacity-60"
                  >
                    {t("restaurar")}
                  </button>
                ) : (
                  <button
                    type="button"
                    disabled={trabajando === resena.id}
                    onClick={() =>
                      accion(`/admin/moderacion/resenas/${resena.id}/ocultar`, resena.id)
                    }
                    data-testid="ocultar-resena"
                    className="press min-h-touch w-fit rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text disabled:opacity-60"
                  >
                    {t("ocultar")}
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
