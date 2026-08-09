import { getTranslations } from "next-intl/server";

import { TarjetaEvento } from "@/components/agenda/TarjetaEvento";
import { Link } from "@/i18n/navegacion";
import { proximosEventos } from "@/lib/eventos";

/**
 * "Proximos eventos" de la portada, con cuenta regresiva (RF-84).
 *
 * <p>Si no hay ninguno —o si el API no responde— la seccion <strong>no se
 * pinta</strong>. Una portada con un titulo y un hueco debajo se ve peor que una
 * portada sin esa seccion.</p>
 */
export async function ProximosEventos({ idioma }: { idioma: string }) {
  const t = await getTranslations("agenda");
  const eventos = await proximosEventos(idioma, 3);

  if (eventos.length === 0) {
    return null;
  }

  return (
    <section className="flex flex-col gap-4" data-testid="proximos-eventos">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-fluid-xl font-semibold text-text">{t("proximosEventos")}</h2>
        <Link
          href="/agenda"
          className="press text-fluid-sm font-medium text-accent underline-offset-4 hover:underline"
        >
          {t("verCalendario")}
        </Link>
      </div>

      <ul className="flex flex-col gap-3">
        {eventos.map((evento) => (
          <TarjetaEvento key={evento.id} evento={evento} conCuentaRegresiva />
        ))}
      </ul>
    </section>
  );
}
