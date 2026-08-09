"use client";

import { useTranslations } from "next-intl";

import { env } from "@/lib/env";
import type { Negocio } from "@/types/negocio";

/**
 * Contacto por WhatsApp y como llegar (RF-110), con su analitica.
 *
 * <p>Los dos clics se anotan en la analitica agregada del Bloque 10. Se cuentan
 * <strong>sin saber quien hizo clic</strong>: el backend usa la misma huella
 * HMAC efimera del anonimato del Bloque 8 y una ventana de 30 minutos, asi que
 * pulsar dos veces no infla el numero y no queda rastro de la persona.</p>
 *
 * <p>El aviso se envia con {@code keepalive}: sin el, la peticion se cancelaria
 * al salir la pestana hacia WhatsApp y el clic no llegaria a contarse nunca —que
 * es justo el clic que mas le importa al negocio—.</p>
 */
export function BotonesDeContacto({ negocio }: { negocio: Negocio }) {
  const t = useTranslations("negocios");

  function anotar(interaccion: "WHATSAPP" | "COMO_LLEGAR") {
    try {
      fetch(`${env.apiUrl}/analitica/negocios/${negocio.id}/${interaccion}`, {
        method: "POST",
        keepalive: true,
      }).catch(() => {
        // Que falle la metrica no puede impedir el contacto.
      });
    } catch {
      /* ignorado a proposito */
    }
  }

  const mensaje = t("mensajeWhatsapp", { negocio: negocio.nombre });

  return (
    <div className="flex flex-wrap gap-2" data-testid="contacto-negocio">
      {negocio.whatsapp && (
        <a
          href={`https://wa.me/${negocio.whatsapp}?text=${encodeURIComponent(mensaje)}`}
          target="_blank"
          rel="noopener noreferrer"
          onClick={() => anotar("WHATSAPP")}
          data-testid="boton-whatsapp"
          className="press min-h-touch rounded-card bg-primary px-5 py-2 text-fluid-sm font-medium text-primary-fg"
        >
          {t("escribirPorWhatsapp")}
        </a>
      )}

      {negocio.latitud !== null && negocio.longitud !== null && (
        <a
          href={`https://www.google.com/maps/dir/?api=1&destination=${negocio.latitud},${negocio.longitud}`}
          target="_blank"
          rel="noopener noreferrer"
          onClick={() => anotar("COMO_LLEGAR")}
          data-testid="boton-como-llegar"
          className="press min-h-touch rounded-card border border-border-strong px-5 py-2 text-fluid-sm font-medium text-text"
        >
          {t("comoLlegar")}
        </a>
      )}

      {negocio.telefono && (
        <a
          href={`tel:${negocio.telefono.replace(/[^0-9+]/g, "")}`}
          className="press min-h-touch rounded-card border border-border-strong px-5 py-2 text-fluid-sm font-medium text-text"
        >
          {t("llamar")}
        </a>
      )}
    </div>
  );
}
