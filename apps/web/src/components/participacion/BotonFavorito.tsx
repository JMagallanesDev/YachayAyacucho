"use client";

import { motion, useReducedMotion } from "motion/react";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { alternarFavorito, esFavorito } from "@/lib/participacion";
import { useSesion } from "@/stores/sesion";

/**
 * Guardar un lugar (RF-35) con microinteraccion (RF-95).
 *
 * <p><strong>Optimista a proposito.</strong> El corazon cambia en el mismo
 * fotograma del toque y la peticion sale despues; si falla, se revierte. Es lo
 * que exige el RF-95 —menos de 16 ms de respuesta—: esperar la ida y vuelta al
 * servidor daria un retardo perceptible en una accion que debe sentirse
 * instantanea.</p>
 *
 * <p>Respeta {@code prefers-reduced-motion}: quien lo tenga activado ve el
 * cambio de color sin el rebote.</p>
 */
export function BotonFavorito({ slug, compacto = false }: { slug: string; compacto?: boolean }) {
  const t = useTranslations("participacion");
  const usuario = useSesion((estado) => estado.usuario);
  const sinMovimiento = useReducedMotion();

  const [marcado, setMarcado] = useState(false);
  const [ocupado, setOcupado] = useState(false);

  useEffect(() => {
    if (usuario) {
      void esFavorito(slug).then(setMarcado);
    }
  }, [usuario, slug]);

  if (!usuario) {
    return null;
  }

  async function alternar(evento: React.MouseEvent) {
    // La tarjeta entera es un enlace: sin esto, marcar favorito navegaria a la
    // ficha del lugar.
    evento.preventDefault();
    evento.stopPropagation();

    if (ocupado) return;

    const previo = marcado;
    setMarcado(!previo);
    setOcupado(true);

    try {
      const resultado = await alternarFavorito(slug);
      setMarcado(resultado);
    } catch {
      setMarcado(previo);
    } finally {
      setOcupado(false);
    }
  }

  return (
    <motion.button
      type="button"
      onClick={alternar}
      aria-pressed={marcado}
      aria-label={marcado ? t("quitarFavorito") : t("guardarFavorito")}
      data-testid="boton-favorito"
      data-marcado={marcado}
      whileTap={sinMovimiento ? undefined : { scale: 0.85 }}
      animate={sinMovimiento ? undefined : { scale: marcado ? [1, 1.25, 1] : 1 }}
      transition={{ duration: 0.28 }}
      className={`press inline-flex items-center justify-center rounded-full ${
        compacto ? "size-9" : "min-h-touch min-w-touch"
      } ${marcado ? "text-primary" : "text-text-muted"}`}
    >
      <span aria-hidden="true" className="text-fluid-xl">
        {marcado ? "♥" : "♡"}
      </span>
    </motion.button>
  );
}
