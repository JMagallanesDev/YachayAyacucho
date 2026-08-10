"use client";

import { useId, useState } from "react";
import { useTranslations } from "next-intl";

import type { ImagenHistorica } from "@/types/negocio";

/**
 * Comparacion antes/despues de una foto historica (RF-11).
 *
 * <p><strong>El control es un `input type="range"` de verdad</strong>, estilado
 * como el tirador del divisor. Parece un detalle de implementacion y es la
 * decision mas importante del componente: un `div` con eventos de puntero se ve
 * igual, pero deja fuera a quien navega con teclado y no dice nada a un lector
 * de pantalla. Con un range nativo se obtienen gratis el foco, las flechas, el
 * arrastre tactil y el anuncio del valor.</p>
 *
 * <p>Si no hay foto actual <strong>no se pinta ningun slider</strong>: se
 * muestra la historica sola con su año y su credito. Una comparacion con un solo
 * lado no es una comparacion, y fingirla con un hueco gris seria peor que no
 * ofrecerla.</p>
 */
export function SliderAntesDespues({
  imagen,
  alturaClase = "h-[60svh]",
}: {
  imagen: ImagenHistorica;
  alturaClase?: string;
}) {
  const t = useTranslations("patrimonio");
  const [posicion, setPosicion] = useState(50);
  const idControl = useId();

  const pie = (
    <figcaption className="flex flex-col gap-1 px-1 pt-2">
      <span className="text-fluid-sm font-medium text-text">{imagen.titulo}</span>
      {imagen.creditoHistorico && (
        <span className="text-fluid-sm text-text-muted">{imagen.creditoHistorico}</span>
      )}
    </figcaption>
  );

  // Degradacion elegante: sin contraparte moderna, solo la foto antigua.
  if (!imagen.urlActual) {
    return (
      <figure data-testid="historia-sin-comparacion" data-imagen={imagen.id}>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={imagen.urlHistorica}
          alt={imagen.titulo}
          className={`w-full rounded-card object-cover ${alturaClase}`}
        />
        <span className="mt-2 inline-block rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted">
          {imagen.anioHistorico}
        </span>
        {pie}
      </figure>
    );
  }

  return (
    <figure data-testid="slider-antes-despues" data-imagen={imagen.id}>
      <div className={`relative select-none overflow-hidden rounded-card ${alturaClase}`}>
        {/* Debajo, la foto actual: es la que se ve al llevar el divisor a la
            izquierda del todo. */}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={imagen.urlActual}
          alt={t("fotoActual", { titulo: imagen.titulo })}
          className="absolute inset-0 size-full object-cover"
          draggable={false}
        />

        {/* Encima, la historica, recortada por el divisor. `clip-path` y no
            `width`: recortando el contenedor la imagen no se deforma al
            estrecharse, que es lo que pasa si se anima el ancho. */}
        <div
          className="absolute inset-0"
          style={{ clipPath: `inset(0 ${100 - posicion}% 0 0)` }}
          aria-hidden="true"
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={imagen.urlHistorica}
            alt=""
            className="size-full object-cover"
            draggable={false}
          />
        </div>

        {/* La linea del divisor. No captura el puntero: quien lo hace es el
            range invisible que va encima de todo. */}
        <div
          className="pointer-events-none absolute inset-y-0 w-0.5 bg-sobre-foto-solido shadow-card"
          style={{ left: `${posicion}%` }}
          aria-hidden="true"
        >
          <span className="absolute top-1/2 left-1/2 flex size-10 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full bg-sobre-foto-solido text-anil-800 shadow-card">
            ↔
          </span>
        </div>

        <span className="pointer-events-none absolute top-3 left-3 rounded-full bg-sobre-foto-bg px-2.5 py-1 text-fluid-sm font-medium text-sobre-foto-fg">
          {imagen.anioHistorico}
        </span>
        <span className="pointer-events-none absolute top-3 right-3 rounded-full bg-sobre-foto-bg px-2.5 py-1 text-fluid-sm font-medium text-sobre-foto-fg">
          {t("hoy")}
        </span>

        <label htmlFor={idControl} className="sr-only">
          {t("etiquetaDivisor")}
        </label>
        <input
          id={idControl}
          type="range"
          min={0}
          max={100}
          value={posicion}
          onChange={(e) => setPosicion(Number(e.target.value))}
          data-testid="divisor-slider"
          aria-valuetext={t("valorDivisor", { porcentaje: posicion })}
          className="absolute inset-0 size-full cursor-ew-resize appearance-none bg-transparent
                     [&::-webkit-slider-thumb]:h-full [&::-webkit-slider-thumb]:w-10
                     [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:opacity-0
                     [&::-moz-range-thumb]:h-full [&::-moz-range-thumb]:w-10
                     [&::-moz-range-thumb]:border-0 [&::-moz-range-thumb]:opacity-0"
        />
      </div>
      {pie}
    </figure>
  );
}
