"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { useTranslations } from "next-intl";

import type { FechaISO } from "@/lib/fechas";
import { COOKIE_VIAJE, DIAS_DE_VIDA_COOKIE } from "@/lib/viaje";

/**
 * Selector de fechas de viaje.
 *
 * <p><strong>Se guarda en una cookie y no en localStorage</strong>, y la
 * diferencia importa: el servidor puede leer una cookie, de modo que la pagina
 * llega ya renderizada con los eventos del viaje. Con localStorage el servidor
 * no sabria nada, habria que pintar un esqueleto y rellenarlo despues, y el
 * visitante veria un parpadeo cada vez que entra. El plan original decia
 * localStorage; la cookie es mejor decision por esto.</p>
 *
 * <p>No lleva `httpOnly` a proposito: la escribe este componente desde el
 * navegador. Tampoco es un dato sensible —dos fechas— y no da acceso a nada.</p>
 *
 * <p>Los campos son `input type="date"` nativos: en un movil abren el
 * calendario del sistema, que es mejor que cualquier selector propio y no
 * cuesta ni una libreria.</p>
 */
export function SelectorFechasViaje({
  desde,
  hasta,
}: {
  desde: FechaISO;
  hasta: FechaISO;
}) {
  const t = useTranslations("agenda");
  const router = useRouter();
  const [pendiente, iniciar] = useTransition();

  const [llegada, setLlegada] = useState(desde);
  const [regreso, setRegreso] = useState(hasta);

  const rangoInvertido = regreso < llegada;

  function guardar(evento: React.FormEvent) {
    evento.preventDefault();
    if (rangoInvertido) {
      return;
    }

    document.cookie = `${COOKIE_VIAJE}=${llegada}..${regreso}; path=/; max-age=${
      DIAS_DE_VIDA_COOKIE * 24 * 60 * 60
    }; SameSite=Lax`;

    // El servidor vuelve a renderizar leyendo la cookie recien escrita.
    iniciar(() => router.refresh());
  }

  return (
    <form
      onSubmit={guardar}
      data-testid="selector-fechas-viaje"
      className="flex flex-col gap-4 rounded-card border border-border-base bg-surface p-5"
    >
      <div className="flex flex-wrap gap-4">
        <label className="flex flex-1 flex-col gap-1">
          <span className="text-fluid-sm text-text-muted">{t("llegada")}</span>
          <input
            type="date"
            value={llegada}
            onChange={(e) => setLlegada(e.target.value)}
            required
            data-testid="fecha-llegada"
            className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
          />
        </label>

        <label className="flex flex-1 flex-col gap-1">
          <span className="text-fluid-sm text-text-muted">{t("regreso")}</span>
          <input
            type="date"
            value={regreso}
            min={llegada}
            onChange={(e) => setRegreso(e.target.value)}
            required
            data-testid="fecha-regreso"
            className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
          />
        </label>
      </div>

      {rangoInvertido && (
        <p role="alert" data-testid="error-rango" className="text-fluid-sm text-text">
          {t("rangoInvertido")}
        </p>
      )}

      <button
        type="submit"
        disabled={rangoInvertido || pendiente}
        data-testid="aplicar-fechas"
        className="press min-h-touch w-fit rounded-card bg-primary px-5 py-2 text-fluid-sm font-medium text-primary-fg disabled:opacity-50"
      >
        {pendiente ? t("buscando") : t("verQueHay")}
      </button>
    </form>
  );
}
