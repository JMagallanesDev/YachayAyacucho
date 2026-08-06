"use client";

import { AnimatePresence, motion } from "motion/react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { ErrorApi } from "@/lib/auth";
import { registrarCheckIn } from "@/lib/participacion";
import { useSesion } from "@/stores/sesion";
import type { CheckInResultado } from "@/types/participacion";

/**
 * Registrar una visita (RF-39).
 *
 * <p>La posicion se pide <strong>solo al pulsar</strong>, nunca al cargar la
 * pagina: un dialogo de permiso que aparece sin que nadie lo haya pedido se
 * deniega, y en geolocalizacion una denegacion es permanente en ese navegador.
 * Es el mismo criterio del Bloque 5.</p>
 *
 * <p>El resultado se celebra en el momento con las insignias recien ganadas,
 * que vienen en la propia respuesta: pedirlas aparte haria que el logro
 * llegara tarde.</p>
 */
export function BotonCheckIn({ slug }: { slug: string }) {
  const t = useTranslations("participacion");
  const usuario = useSesion((estado) => estado.usuario);

  const [estado, setEstado] = useState<"listo" | "ubicando" | "enviando">("listo");
  const [resultado, setResultado] = useState<CheckInResultado | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!usuario) {
    return null;
  }

  async function hacerCheckIn() {
    setError(null);
    setEstado("ubicando");

    if (!navigator.geolocation) {
      setError(t("sinGeolocalizacion"));
      setEstado("listo");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (posicion) => {
        setEstado("enviando");
        try {
          const registrado = await registrarCheckIn(slug, {
            longitud: posicion.coords.longitude,
            latitud: posicion.coords.latitude,
            // El servidor la usa para descartar lecturas malas.
            precision: posicion.coords.accuracy,
          });
          setResultado(registrado);
        } catch (fallo) {
          setError(
            fallo instanceof ErrorApi
              ? (fallo.problema?.detail ?? t("errorCheckIn"))
              : t("errorCheckIn"),
          );
        } finally {
          setEstado("listo");
        }
      },
      () => {
        // Permiso denegado o sin senal: se distingue del error del servidor
        // porque la solucion es distinta.
        setError(t("sinPermisoUbicacion"));
        setEstado("listo");
      },
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 30000 },
    );
  }

  return (
    <section className="flex flex-col gap-3" data-testid="seccion-checkin">
      {!resultado && (
        <button
          type="button"
          onClick={hacerCheckIn}
          disabled={estado !== "listo"}
          data-testid="boton-checkin"
          className="press min-h-touch w-fit rounded-card bg-secondary px-5 text-fluid-sm font-medium text-secondary-fg disabled:opacity-60"
        >
          {estado === "ubicando"
            ? t("ubicando")
            : estado === "enviando"
              ? t("registrando")
              : t("estuveAqui")}
        </button>
      )}

      <p className="text-fluid-sm text-text-muted">{t("explicacionCheckIn")}</p>

      {error && (
        <p role="alert" data-testid="error-checkin" className="rounded-card bg-danger-subtle p-3 text-fluid-sm text-text">
          {error}
        </p>
      )}

      <AnimatePresence>
        {resultado && (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            data-testid="checkin-exitoso"
            className="flex flex-col gap-2 rounded-card border border-border-base bg-surface p-4"
            role="status"
          >
            <strong className="text-fluid-lg text-text">{t("selloConseguido")}</strong>
            <p className="text-fluid-sm text-text-muted">
              {t("sellosTotales", { total: resultado.sellos })}
            </p>

            {resultado.insigniasNuevas.length > 0 && (
              <ul className="flex flex-wrap gap-2" data-testid="insignias-nuevas">
                {resultado.insigniasNuevas.map((codigo) => (
                  <li
                    key={codigo}
                    data-testid={`insignia-nueva-${codigo}`}
                    className="rounded-full bg-accent-subtle px-3 py-1 text-fluid-sm font-medium text-text"
                  >
                    {t("insigniaGanada", { nombre: t(`insignia.${codigo}`) })}
                  </li>
                ))}
              </ul>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </section>
  );
}
