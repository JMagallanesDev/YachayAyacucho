"use client";

import { useTranslations } from "next-intl";

/**
 * Calificacion de 1 a 5 estrellas.
 *
 * <p>Cuando es interactiva se construye con <strong>radios reales</strong> y no
 * con botones: un grupo de radio es lo que un lector de pantalla anuncia como
 * «opcion 4 de 5», y se recorre con las flechas del teclado sin escribir una
 * sola linea para ello. Las estrellas visibles son el `label` de cada radio.</p>
 */
export function Estrellas({
  valor,
  alCambiar,
  nombre = "calificacion",
  tamano = "md",
}: {
  valor: number;
  alCambiar?: (valor: number) => void;
  nombre?: string;
  tamano?: "sm" | "md";
}) {
  const t = useTranslations("resenas");
  const interactiva = typeof alCambiar === "function";
  const clase = tamano === "sm" ? "text-fluid-sm" : "text-fluid-xl";

  if (!interactiva) {
    return (
      <span className={`${clase} text-accent`} aria-label={t("deCinco", { nota: valor })}>
        <span aria-hidden="true">{"★".repeat(valor)}</span>
        <span aria-hidden="true" className="text-border-strong">
          {"★".repeat(5 - valor)}
        </span>
      </span>
    );
  }

  return (
    <fieldset className="flex items-center gap-1" data-testid="selector-estrellas">
      <legend className="sr-only">{t("tuCalificacion")}</legend>
      {[1, 2, 3, 4, 5].map((n) => (
        <label
          key={n}
          className={`cursor-pointer ${clase} ${n <= valor ? "text-accent" : "text-border-strong"}`}
        >
          <input
            type="radio"
            name={nombre}
            value={n}
            checked={valor === n}
            onChange={() => alCambiar(n)}
            className="sr-only"
            data-testid={`estrella-${n}`}
          />
          <span aria-hidden="true">★</span>
          <span className="sr-only">{t("deCinco", { nota: n })}</span>
        </label>
      ))}
    </fieldset>
  );
}
