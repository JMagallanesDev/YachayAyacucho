"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";

import { useProximidad } from "@/hooks/useProximidad";
import type { ImagenHistorica } from "@/types/negocio";

import { SliderAntesDespues } from "./SliderAntesDespues";

/**
 * Historia visual de un lugar, con el modo «Parate aqui» (RF-11, RF-11b).
 *
 * <p><strong>Reutiliza {@code useProximidad} tal cual</strong>, el hook del
 * Bloque 5, alimentandolo con los puntos de captura de las fotos en vez de con
 * lugares. De ahi hereda las tres decisiones que lo hacen usable en la calle: la
 * histeresis 50/80 m para que el aviso no parpadee en el borde, la supresion de
 * dos horas para no repetirlo a quien esta sentado en la plaza, y el arranque
 * solo tras un gesto explicito, porque {@code watchPosition} vacia la bateria y
 * un permiso pedido sin contexto se deniega para siempre.</p>
 *
 * <p><strong>El encuadre honesto, el mismo que el check-in del Bloque 7:</strong>
 * el GPS de un navegador se falsea en dos clics. Aqui da igual, porque esto no
 * desbloquea nada: es una invitacion a mirar, no una credencial.</p>
 */
export function HistoriaVisual({ imagenes }: { imagenes: ImagenHistorica[] }) {
  const t = useTranslations("patrimonio");
  const [rastreando, setRastreando] = useState(false);
  const [aPantallaCompleta, setAPantallaCompleta] = useState<string | null>(null);

  // Solo se puede «llegar» a una foto que tenga punto conocido Y contraparte
  // actual: mandar a alguien a caminar hasta un sitio para no ensenarle una
  // comparacion seria una broma pesada.
  const conPunto = imagenes.filter(
    (imagen) => imagen.latitudCaptura !== null && imagen.longitudCaptura !== null && imagen.urlActual,
  );

  const { cercano, descartar } = useProximidad(
    conPunto.map((imagen) => ({
      slug: imagen.id,
      nombre: imagen.titulo,
      latitud: imagen.latitudCaptura as number,
      longitud: imagen.longitudCaptura as number,
    })),
    rastreando,
  );

  if (imagenes.length === 0) {
    // Degradacion elegante: la mayoria de los lugares no tienen foto antigua
    // localizable, y la seccion sencillamente no se pinta.
    return null;
  }

  const imagenAmpliada = imagenes.find((imagen) => imagen.id === aPantallaCompleta);

  return (
    <section className="flex flex-col gap-4" data-testid="historia-visual">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-fluid-xl font-semibold text-text">{t("tituloHistoriaVisual")}</h2>

        {conPunto.length > 0 && !rastreando && (
          <button
            type="button"
            onClick={() => setRastreando(true)}
            data-testid="activar-parate-aqui"
            className="press min-h-touch rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
          >
            {t("activarParateAqui")}
          </button>
        )}
        {rastreando && (
          <span className="text-fluid-sm text-text-muted" data-testid="rastreando">
            {t("buscandoTuPosicion")}
          </span>
        )}
      </div>

      <p className="text-fluid-sm text-text-muted">{t("descripcionHistoriaVisual")}</p>

      <div className="flex flex-col gap-6">
        {imagenes.map((imagen) => (
          <SliderAntesDespues key={imagen.id} imagen={imagen} />
        ))}
      </div>

      {/* Aviso de proximidad: estas justo donde se tomo la foto antigua. */}
      {cercano && (
        <div
          role="status"
          data-testid="aviso-parate-aqui"
          className="fixed inset-x-4 bottom-6 z-40 flex flex-col gap-3 rounded-card border border-border-strong bg-surface p-4 shadow-card"
          style={{ paddingBottom: "max(1rem, env(safe-area-inset-bottom))" }}
        >
          <strong className="text-fluid-base text-text">{t("estasEnElPunto")}</strong>
          <p className="text-fluid-sm text-text-muted">
            {t("estasEnElPuntoAyuda", { titulo: cercano.nombre })}
          </p>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => {
                setAPantallaCompleta(cercano.slug);
                descartar();
              }}
              data-testid="abrir-comparacion"
              className="press min-h-touch rounded-card bg-primary px-4 text-fluid-sm font-medium text-primary-fg"
            >
              {t("verComparacion")}
            </button>
            <button
              type="button"
              onClick={descartar}
              className="press min-h-touch rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
            >
              {t("ahoraNo")}
            </button>
          </div>
        </div>
      )}

      {/* Pantalla completa. Es un overlay y no una ruta nueva: se abre estando
          de pie en la calle, y navegar a otra pagina obligaria a volver. */}
      {imagenAmpliada && (
        <div
          role="dialog"
          aria-modal="true"
          data-testid="comparacion-pantalla-completa"
          className="fixed inset-0 z-50 flex flex-col gap-3 bg-surface p-4"
          style={{ paddingTop: "max(1rem, env(safe-area-inset-top))" }}
        >
          <button
            type="button"
            onClick={() => setAPantallaCompleta(null)}
            data-testid="cerrar-comparacion"
            className="press min-h-touch w-fit rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
          >
            {t("cerrar")}
          </button>
          <SliderAntesDespues imagen={imagenAmpliada} alturaClase="h-[75svh]" />
        </div>
      )}
    </section>
  );
}
