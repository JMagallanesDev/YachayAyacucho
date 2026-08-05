import { useTranslations } from "next-intl";

import type { Horario } from "@/types/lugar";

/** 0 = domingo, tal como lo guarda el backend. */
const DIAS = ["domingo", "lunes", "martes", "miercoles", "jueves", "viernes", "sabado"] as const;

/**
 * Grilla semanal de apertura (RF-09b).
 *
 * <p>Un mismo dia puede tener varios tramos —los templos cierran al mediodia—,
 * asi que se agrupan por dia en vez de asumir un unico horario por jornada.</p>
 */
export function TablaHorarios({ horarios }: { horarios: Horario[] }) {
  const t = useTranslations("lugares");

  if (horarios.length === 0) {
    return null;
  }

  return (
    <section data-testid="horarios" className="flex flex-col gap-4">
      <h2 className="text-fluid-xl font-semibold text-text">{t("horarios")}</h2>
      <dl className="flex flex-col divide-y divide-border-base rounded-card bg-surface-muted px-4">
        {DIAS.map((dia, indice) => {
          const tramos = horarios.filter((horario) => horario.diaSemana === indice);
          const abiertos = tramos.filter((tramo) => !tramo.cerrado);

          return (
            <div key={dia} className="flex items-baseline justify-between gap-3 py-3">
              <dt className="text-fluid-sm text-text-muted">{t(`dia.${dia}`)}</dt>
              <dd className="text-right text-fluid-sm font-medium text-text">
                {abiertos.length === 0
                  ? t("cerrado")
                  : abiertos
                      .map((tramo) => `${recortar(tramo.horaApertura)}–${recortar(tramo.horaCierre)}`)
                      .join(" · ")}
              </dd>
            </div>
          );
        })}
      </dl>
    </section>
  );
}

/** El backend serializa LocalTime como "09:00:00"; sobran los segundos. */
function recortar(hora: string | null): string {
  return hora ? hora.slice(0, 5) : "";
}
