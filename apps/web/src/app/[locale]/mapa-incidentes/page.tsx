import { getTranslations, setRequestLocale } from "next-intl/server";

import { Link } from "@/i18n/navegacion";
import { Aparecer } from "@/components/movimiento/Aparecer";
import { LIMITES_AYACUCHO } from "@/lib/mapa";
import { incidentesEnMapa } from "@/lib/reportes";

import { MapaIncidentesCargable } from "./MapaIncidentesCargable";
import Image from "next/image";
import { TAMANOS, cargadorCloudinary } from "@/lib/imagenes";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "incidentes" });
  return { title: t("titulo"), description: t("descripcion") };
}

/**
 * Mapa publico de incidentes (RF-74).
 *
 * <p>Solo llegan aqui los reportes <strong>aprobados o resueltos</strong>. Nada
 * se publica sin que una persona lo haya revisado: es contenido que acusa a
 * terceros de danar el patrimonio, y difundir una denuncia falsa seria el peor
 * resultado posible de este modulo.</p>
 *
 * <p>Debajo del mapa va la misma informacion en HTML, por lo mismo que en el
 * mapa de lugares: un lienzo WebGL es invisible para un buscador y para un
 * lector de pantalla.</p>
 */
export const revalidate = 300;

export default async function PaginaMapaIncidentes({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("incidentes");

  const [oeste, sur, este, norte] = LIMITES_AYACUCHO;
  const incidentes = await incidentesEnMapa({ oeste, sur, este, norte }, locale);

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-5xl flex-col gap-8 px-5 py-10">
      <header className="flex flex-col gap-3">
        <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
        <Link
          href="/reportar"
          className="press min-h-touch w-fit rounded-card bg-primary px-5 py-2 text-fluid-sm font-medium text-primary-fg"
        >
          {t("reportarIncidente")}
        </Link>
      </header>

      <MapaIncidentesCargable incidentes={incidentes} />

      <section className="flex flex-col gap-3">
        <h2 className="text-fluid-xl font-semibold text-text">
          {t("listado", { total: incidentes.length })}
        </h2>

        {incidentes.length === 0 ? (
          <p data-testid="sin-incidentes" className="rounded-card bg-surface-muted p-6 text-text-muted">
            {t("sinIncidentes")}
          </p>
        ) : (
          <ul className="grid gap-3 sm:grid-cols-2" data-testid="lista-incidentes">
            {incidentes.map((incidente, i) => (
              <Aparecer
                comoLista
                indice={i}
                key={incidente.id}
                data-testid="incidente"
                data-estado={incidente.estado}
                className="flex flex-col gap-2 rounded-card border border-border-base bg-surface p-4"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <span
                    className="rounded-full px-2.5 py-1 text-fluid-sm font-medium text-text"
                    /* El color del tipo se queda en el FONDO: como texto no
                       puede garantizar 4.5:1, porque sale del catalogo y no de
                       un token medido (WCAG 1.4.3). */
                    style={{ backgroundColor: `${incidente.colorHex}2e` }}
                  >
                    {incidente.tipoNombre}
                  </span>
                  <span className="text-fluid-sm text-text-muted">
                    {t(`estado.${incidente.estado}`)}
                  </span>
                </div>

                <p className="text-fluid-base text-text">{incidente.descripcion}</p>

                {incidente.direccionReferencial && (
                  <p className="text-fluid-sm text-text-muted">{incidente.direccionReferencial}</p>
                )}

                {incidente.fotos.length > 0 && (
                  <Image
                    src={incidente.fotos[0]}
                    alt=""
                    width={400}
                    height={160}
                    sizes={TAMANOS.tarjeta}
                    loader={cargadorCloudinary}
                    className="h-40 w-full rounded-card object-cover"
                  />
                )}
              </Aparecer>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
