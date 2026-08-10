"use client";

import { motion, AnimatePresence } from "motion/react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { useProximidad, type LugarCercano } from "@/hooks/useProximidad";
import { Link } from "@/i18n/navegacion";
import { useUbicacion } from "@/stores/ubicacion";

/**
 * Aviso «estas en...» al llegar a un lugar (RF-19b).
 *
 * <p>Aparece anclado abajo, sobre la barra de gestos del sistema, respetando el
 * area segura del dispositivo. Se puede descartar siempre: un aviso que no se
 * puede quitar deja de ser una ayuda.</p>
 *
 * <p>El seguimiento solo arranca cuando la persona lo activa. Vease la
 * explicacion en {@code useProximidad}.</p>
 */
export function BannerProximidad({ lugares }: { lugares: LugarCercano[] }) {
  const t = useTranslations("proximidad");
  const [activo, setActivo] = useState(false);
  const permiso = useUbicacion((estado) => estado.permiso);
  const { cercano, descartar } = useProximidad(lugares, activo);

  return (
    <>
      {!activo && permiso !== "denegado" && (
        <button
          type="button"
          onClick={() => setActivo(true)}
          data-testid="activar-proximidad"
          className="press min-h-touch w-fit rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
        >
          {t("activar")}
        </button>
      )}

      {activo && !cercano && (
        <p className="text-fluid-sm text-text-muted" data-testid="proximidad-activa">
          {t("buscando")}
        </p>
      )}

      <AnimatePresence>
        {cercano && (
          <motion.div
            initial={{ y: 80, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: 80, opacity: 0 }}
            transition={{ type: "spring", stiffness: 320, damping: 30 }}
            data-testid="banner-proximidad"
            data-slug={cercano.slug}
            className="fixed inset-x-4 z-50 flex items-center gap-3 rounded-card border border-border-base bg-surface p-4 shadow-card"
            // Sobre la barra de gestos, no debajo de ella.
            style={{ bottom: "max(1rem, env(safe-area-inset-bottom))" }}
            role="status"
          >
            <div className="flex flex-col gap-0.5">
              <span className="text-fluid-sm text-text-muted">{t("estasEn")}</span>
              <strong className="text-fluid-lg text-text">{cercano.nombre}</strong>
            </div>

            <Link
              href={`/lugares/${cercano.slug}`}
              className="press ms-auto min-h-touch rounded-card bg-primary px-4 py-2 text-fluid-sm font-medium text-primary-fg"
            >
              {t("verFicha")}
            </Link>

            <button
              type="button"
              onClick={descartar}
              aria-label={t("descartar")}
              className="press min-h-touch rounded-card px-2 text-text-muted"
            >
              ✕
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
