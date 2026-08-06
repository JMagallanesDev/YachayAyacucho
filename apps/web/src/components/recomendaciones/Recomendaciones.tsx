import { useTranslations } from "next-intl";

import { Link } from "@/i18n/navegacion";
import type { Recomendacion } from "@/types/clima";

/**
 * «¿Que hago ahora?» (RF-08).
 *
 * <p>Cada sugerencia muestra <strong>por que</strong> se propone. Es lo que
 * separa una recomendacion util de una caja negra: quien lee «esta abierto y a
 * cubierto de la lluvia» entiende la logica y puede estar en desacuerdo con
 * ella, que es justo lo que permite confiar en el resto.</p>
 *
 * <p>Los motivos llegan del backend como claves de traduccion, nunca como
 * frases ya armadas, para que cambien de idioma con el resto de la interfaz.</p>
 */
export function Recomendaciones({ recomendaciones }: { recomendaciones: Recomendacion[] }) {
  const t = useTranslations("recomendaciones");

  return (
    <section className="flex flex-col gap-4" data-testid="recomendaciones">
      <div className="flex flex-col gap-1">
        <h2 className="text-fluid-xl font-semibold text-text">{t("titulo")}</h2>
        <p className="text-fluid-sm text-text-muted">{t("descripcion")}</p>
      </div>

      {/* Fuera del horario de visita no hay nada que recomendar, y eso hay que
          decirlo. Ocultar la seccion sin mas dejaria un hueco inexplicable a
          quien abra la pagina de noche, que es justo cuando mas gente planea
          el dia siguiente. */}
      {recomendaciones.length === 0 && (
        <p
          data-testid="sin-recomendaciones"
          className="rounded-card bg-surface-muted p-4 text-fluid-sm text-text-muted"
        >
          {t("nadaAbierto")}
        </p>
      )}

      <ul className="grid gap-3 sm:grid-cols-2">
        {recomendaciones.map((recomendacion) => (
          <li key={recomendacion.lugar.id}>
            <Link
              href={`/lugares/${recomendacion.lugar.slug}`}
              data-testid="recomendacion"
              data-slug={recomendacion.lugar.slug}
              className="press flex h-full flex-col gap-2 rounded-card border border-border-base bg-surface p-4 shadow-card"
            >
              <span
                className="w-fit rounded-full px-2.5 py-1 text-fluid-sm font-medium"
                style={{
                  backgroundColor: `${recomendacion.lugar.categoria.colorHex}1a`,
                  color: recomendacion.lugar.categoria.colorHex,
                }}
              >
                {recomendacion.lugar.categoria.nombre}
              </span>

              <h3 className="text-fluid-lg font-semibold text-text">
                {recomendacion.lugar.nombre}
              </h3>

              <ul className="mt-auto flex flex-wrap gap-1.5" data-testid="motivos">
                {recomendacion.motivos.map((motivo) => (
                  <li
                    key={motivo}
                    className="rounded-full bg-surface-muted px-2 py-0.5 text-fluid-sm text-text-muted"
                  >
                    {t(`motivo.${motivo}`)}
                  </li>
                ))}
              </ul>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
