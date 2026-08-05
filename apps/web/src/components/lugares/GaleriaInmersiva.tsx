"use client";

import { useGesture } from "@use-gesture/react";
import useEmblaCarousel from "embla-carousel-react";
import { motion } from "motion/react";
import { useTranslations } from "next-intl";
import { useCallback, useEffect, useRef, useState } from "react";

/**
 * Galeria con swipe y pinch-zoom (RF-10).
 *
 * <p>Embla aporta el desplazamiento con inercia, que es lo que separa un
 * carrusel que se siente nativo de uno que salta de imagen en imagen.</p>
 *
 * <p>El zoom se hace con `@use-gesture` sobre un elemento de `motion`. El
 * detalle que lo hace funcionar en un movil de verdad: mientras hay zoom se
 * <strong>desactiva el arrastre de Embla</strong>. Si no, separar dos dedos
 * sobre la foto pasaria a la siguiente imagen en lugar de ampliar.</p>
 */
export function GaleriaInmersiva({
  imagenes,
  titulo,
}: {
  imagenes: { url: string; alt: string }[];
  titulo: string;
}) {
  const t = useTranslations("lugares");
  const [emblaRef, emblaApi] = useEmblaCarousel({ loop: false, align: "center" });
  const [actual, setActual] = useState(0);
  const [escala, setEscala] = useState(1);
  const contenedor = useRef<HTMLDivElement>(null);

  const alCambiar = useCallback(() => {
    if (emblaApi) {
      setActual(emblaApi.selectedScrollSnap());
      // Cambiar de foto reinicia el zoom: quedarse ampliado en la siguiente
      // desorienta.
      setEscala(1);
    }
  }, [emblaApi]);

  useEffect(() => {
    if (!emblaApi) return;
    emblaApi.on("select", alCambiar);
    return () => {
      emblaApi.off("select", alCambiar);
    };
  }, [emblaApi, alCambiar]);

  // Mientras la foto esta ampliada, el arrastre horizontal debe mover la
  // imagen, no el carrusel.
  useEffect(() => {
    emblaApi?.reInit({ watchDrag: escala <= 1 });
  }, [emblaApi, escala]);

  useGesture(
    {
      onPinch: ({ offset: [d] }) => setEscala(Math.min(3, Math.max(1, 1 + d / 200))),
      onDoubleClick: () => setEscala((previa) => (previa > 1 ? 1 : 2)),
    },
    { target: contenedor, eventOptions: { passive: false } },
  );

  if (imagenes.length === 0) {
    return null;
  }

  return (
    <section aria-label={t("galeria", { titulo })} className="flex flex-col gap-3">
      <div
        ref={contenedor}
        className="overflow-hidden rounded-card bg-surface-muted"
        // touch-action: sin esto el navegador se queda el gesto de pinza y
        // @use-gesture nunca lo recibe.
        style={{ touchAction: escala > 1 ? "none" : "pan-y" }}
      >
        <div ref={emblaRef} className="overflow-hidden">
          <div className="flex">
            {imagenes.map((imagen, indice) => (
              <div key={imagen.url} className="min-w-0 flex-[0_0_100%]">
                <motion.img
                  src={imagen.url}
                  alt={imagen.alt}
                  animate={{ scale: indice === actual ? escala : 1 }}
                  transition={{ type: "spring", stiffness: 260, damping: 30 }}
                  className="h-64 w-full object-cover sm:h-96"
                  draggable={false}
                />
              </div>
            ))}
          </div>
        </div>
      </div>

      {imagenes.length > 1 && (
        <div className="flex justify-center gap-2" role="tablist" aria-label={t("imagenes")}>
          {imagenes.map((imagen, indice) => (
            <button
              key={imagen.url}
              type="button"
              role="tab"
              aria-selected={indice === actual}
              aria-label={t("irAImagen", { numero: indice + 1 })}
              onClick={() => emblaApi?.scrollTo(indice)}
              className={`size-2.5 rounded-full transition-colors ${
                indice === actual ? "bg-primary" : "bg-border-strong"
              }`}
            />
          ))}
        </div>
      )}
    </section>
  );
}
