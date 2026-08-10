"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import type { Ruta } from "@/types/mapa";

/**
 * Controles superpuestos: toggles de categoria (RF-22), rutas (RF-20) y 2D/3D
 * (RF-17).
 *
 * <p>En movil arrancan plegados. Un panel de ocho categorias sobre una pantalla
 * de telefono taparia justo lo que se quiere mirar.</p>
 */
export function ControlesMapa({
  categorias,
  categoriasOcultas,
  alAlternarCategoria,
  rutas,
  rutaVisible,
  alElegirRuta,
  tresD,
  alAlternarTresD,
}: {
  categorias: { codigo: string; nombre: string; color: string }[];
  categoriasOcultas: string[];
  alAlternarCategoria: (codigo: string) => void;
  rutas: Ruta[];
  rutaVisible: string | null;
  alElegirRuta: (slug: string | null) => void;
  tresD: boolean;
  alAlternarTresD: () => void;
  idioma: string;
}) {
  const t = useTranslations("mapa");
  const [abierto, setAbierto] = useState(false);

  return (
    <div className="pointer-events-none absolute inset-x-3 top-3 flex flex-col gap-2">
      <div className="pointer-events-auto flex flex-wrap gap-2">
        <button
          type="button"
          onClick={alAlternarTresD}
          aria-pressed={tresD}
          data-testid="toggle-3d"
          className="press min-h-touch rounded-card bg-surface/95 px-4 text-fluid-sm font-medium text-text shadow-card backdrop-blur"
        >
          {tresD ? t("ver2D") : t("ver3D")}
        </button>

        <button
          type="button"
          onClick={() => setAbierto((previo) => !previo)}
          aria-expanded={abierto}
          data-testid="toggle-filtros"
          className="press min-h-touch rounded-card bg-surface/95 px-4 text-fluid-sm font-medium text-text shadow-card backdrop-blur"
        >
          {t("filtros")}
        </button>
      </div>

      {abierto && (
        <div
          className="pointer-events-auto flex max-h-[45svh] flex-col gap-4 overflow-y-auto rounded-card bg-surface/95 p-4 shadow-card backdrop-blur"
          // Sin esto, al llegar al final de la lista el desplazamiento
          // continuaria en la pagina de debajo.
          style={{ overscrollBehavior: "contain" }}
        >
          <fieldset className="flex flex-col gap-2">
            <legend className="text-fluid-sm font-semibold text-text">{t("categorias")}</legend>
            <div className="flex flex-wrap gap-2">
              {categorias.map((categoria) => {
                const visible = !categoriasOcultas.includes(categoria.codigo);
                return (
                  <button
                    key={categoria.codigo}
                    type="button"
                    onClick={() => alAlternarCategoria(categoria.codigo)}
                    aria-pressed={visible}
                    data-testid={`mapa-categoria-${categoria.codigo}`}
                    className={`press min-h-touch rounded-card border px-3 text-fluid-sm font-medium transition-opacity ${
                      visible ? "opacity-100" : "opacity-40"
                    }`}
                    style={{
                      borderColor: categoria.color,
                      backgroundColor: visible ? `${categoria.color}1a` : "transparent",
                      color: categoria.color,
                    }}
                  >
                    {categoria.nombre}
                  </button>
                );
              })}
            </div>
          </fieldset>

          {rutas.length > 0 && (
            <fieldset className="flex flex-col gap-2">
              <legend className="text-fluid-sm font-semibold text-text">{t("rutas")}</legend>
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => alElegirRuta(null)}
                  aria-pressed={rutaVisible === null}
                  className={`press min-h-touch rounded-card px-3 text-fluid-sm font-medium ${
                    rutaVisible === null
                      ? "bg-primary text-primary-fg"
                      : "bg-surface-muted text-text-muted"
                  }`}
                >
                  {t("ningunaRuta")}
                </button>
                {rutas.map((ruta) => (
                  <button
                    key={ruta.slug}
                    type="button"
                    onClick={() => alElegirRuta(ruta.slug)}
                    aria-pressed={rutaVisible === ruta.slug}
                    data-testid={`mapa-ruta-${ruta.slug}`}
                    className="press min-h-touch rounded-card border px-3 text-fluid-sm font-medium"
                    style={{
                      borderColor: ruta.colorHex,
                      backgroundColor: rutaVisible === ruta.slug ? ruta.colorHex : "transparent",
                      color: rutaVisible === ruta.slug ? "var(--sobre-foto-fg)" : ruta.colorHex,
                    }}
                  >
                    {ruta.nombre}
                    <span className="ms-1.5 font-normal opacity-80">({ruta.paradas.length})</span>
                  </button>
                ))}
              </div>
            </fieldset>
          )}
        </div>
      )}
    </div>
  );
}
