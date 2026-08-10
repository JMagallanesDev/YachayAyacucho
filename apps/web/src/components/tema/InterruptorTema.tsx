"use client";

import { motion, useReducedMotion } from "motion/react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { useValorDelCliente } from "@/components/useValorDelCliente";

import { TEMAS, aplicarTema, leerTema, type Tema } from "./tema";

const ICONO: Record<Tema, string> = {
  claro: "☀",
  oscuro: "☾",
  sistema: "◐",
};

/**
 * Interruptor de tema con tres estados (RF-94).
 *
 * <p><strong>Tres y no dos.</strong> Un interruptor binario obliga a elegir un
 * tema fijo y deja fuera lo que casi todo el mundo quiere: que la aplicacion
 * siga al sistema, que ya cambia solo de dia a noche. "Sistema" es el valor por
 * defecto y no escribe {@code data-theme}, de modo que manda la media query.</p>
 *
 * <p>El valor inicial se lee con {@code useValorDelCliente}: en el servidor no
 * hay {@code localStorage}, y leerlo durante el render seria una discordancia de
 * hidratacion. La instantanea del servidor es "sistema", que es tambien el valor
 * por defecto, asi que el primer fotograma coincide siempre.</p>
 *
 * <p>El indicador se desliza entre las tres opciones con {@code layoutId} de
 * Motion. Es una transicion de elemento compartido: sin ella el fondo saltaria
 * de una opcion a otra, y hacerla a mano seria reimplementar FLIP.</p>
 */
export function InterruptorTema() {
  const t = useTranslations("tema");
  const reducido = useReducedMotion();

  const inicial = useValorDelCliente<Tema>(leerTema, () => "sistema");
  const [tema, setTema] = useState<Tema>(inicial);

  // Si la instantanea del cliente llega despues de hidratar y difiere, se
  // adopta: es el mismo patron del contador del Bloque 9.
  const actual = tema === "sistema" && inicial !== "sistema" ? inicial : tema;

  function elegir(nuevo: Tema) {
    setTema(nuevo);
    aplicarTema(nuevo);
  }

  return (
    <div
      role="radiogroup"
      aria-label={t("etiqueta")}
      data-testid="interruptor-tema"
      data-tema={actual}
      className="flex items-center gap-0.5 rounded-full border border-border-base bg-surface p-0.5"
    >
      {TEMAS.map((opcion) => {
        const activo = actual === opcion;
        return (
          <button
            key={opcion}
            type="button"
            role="radio"
            aria-checked={activo}
            aria-label={t(opcion)}
            title={t(opcion)}
            onClick={() => elegir(opcion)}
            data-testid={`tema-${opcion}`}
            className="press relative flex size-9 min-h-0 min-w-0 items-center justify-center rounded-full text-fluid-sm"
          >
            {activo && (
              <motion.span
                layoutId="indicador-tema"
                aria-hidden="true"
                className="absolute inset-0 rounded-full bg-nav-indicador"
                transition={
                  reducido
                    ? { duration: 0 }
                    : { type: "spring", stiffness: 420, damping: 34 }
                }
              />
            )}
            <span
              className={`relative ${activo ? "text-nav-item-activo" : "text-nav-item"}`}
              aria-hidden="true"
            >
              {ICONO[opcion]}
            </span>
          </button>
        );
      })}
    </div>
  );
}
