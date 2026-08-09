import { getTranslations, setRequestLocale } from "next-intl/server";

import { RegistrarVisita } from "@/components/RegistrarVisita";
import { TarjetaClima } from "@/components/clima/TarjetaClima";
import { BannerProximidad } from "@/components/mapa/BannerProximidad";
import { MapaCargable } from "@/components/mapa/MapaCargable";
import { Recomendaciones } from "@/components/recomendaciones/Recomendaciones";
import { Link } from "@/i18n/navegacion";
import { obtenerClima, obtenerRecomendaciones } from "@/lib/clima";
import { env } from "@/lib/env";
import { lugaresDelMapa, rutasTematicas } from "@/lib/mapa";

/**
 * Mapa del patrimonio (RF-17 a RF-22b), con clima y recomendaciones.
 *
 * <p>Server Component. Resuelve todos los datos y se los entrega ya listos al
 * mapa, que es la unica parte de cliente: asi el mapa no hace ninguna peticion
 * para pintarse.</p>
 *
 * <p>Debajo del mapa se renderiza la <strong>misma informacion en HTML</strong>.
 * No es un adorno: un mapa WebGL es invisible para los buscadores y para un
 * lector de pantalla, asi que la lista es la version accesible e indexable del
 * mismo contenido (RNF de accesibilidad y SEO).</p>
 */
export const revalidate = 300;

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "mapa" });
  return { title: t("titulo"), description: t("descripcion") };
}

export default async function PaginaMapa({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("mapa");

  // En paralelo: ninguno depende de otro y encadenarlos sumaria sus latencias.
  const [lugares, rutas, clima, recomendaciones] = await Promise.all([
    lugaresDelMapa(locale),
    rutasTematicas(locale),
    obtenerClima(),
    obtenerRecomendaciones(locale),
  ]);

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-6xl flex-col gap-8 px-5 py-10">
      <RegistrarVisita tipo="MAPA" />
      <header className="flex flex-col gap-2">
        <h1 className="text-fluid-3xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
      </header>

      <MapaCargable
        lugares={lugares}
        rutas={rutas}
        claveMapTiler={env.maptilerKey}
      />

      <BannerProximidad
        lugares={lugares.features.map((punto) => ({
          slug: punto.properties.slug,
          nombre: punto.properties.nombre,
          longitud: punto.geometry.coordinates[0],
          latitud: punto.geometry.coordinates[1],
        }))}
      />

      <div className="grid gap-8 lg:grid-cols-[1fr_2fr]">
        <TarjetaClima clima={clima} />
        <Recomendaciones recomendaciones={recomendaciones} />
      </div>

      {/* Version accesible del mapa: lo que ve un lector de pantalla. */}
      <section className="flex flex-col gap-3">
        <h2 className="text-fluid-xl font-semibold text-text">{t("listaAccesible")}</h2>
        <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3" data-testid="lista-accesible-mapa">
          {lugares.features.map((punto) => (
            <li key={punto.properties.id}>
              <Link
                href={`/lugares/${punto.properties.slug}`}
                className="flex items-baseline gap-2 rounded-card px-3 py-2 text-fluid-sm text-text hover:bg-surface-muted"
              >
                <span
                  aria-hidden="true"
                  className="size-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: punto.properties.color }}
                />
                <span>{punto.properties.nombre}</span>
                <span className="ml-auto text-text-muted">{punto.properties.categoriaNombre}</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      {rutas.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="text-fluid-xl font-semibold text-text">{t("rutas")}</h2>
          <ul className="flex flex-col gap-3" data-testid="lista-rutas">
            {rutas.map((ruta) => (
              <li
                key={ruta.id}
                className="flex flex-col gap-1 rounded-card border border-border-base bg-surface p-4"
                data-testid={`ruta-${ruta.slug}`}
              >
                <div className="flex items-center gap-2">
                  <span
                    aria-hidden="true"
                    className="h-1 w-8 rounded-full"
                    style={{ backgroundColor: ruta.colorHex }}
                  />
                  <h3 className="text-fluid-lg font-semibold text-text">{ruta.nombre}</h3>
                  <span className="text-fluid-sm text-text-muted">
                    {t("paradas", { total: ruta.paradas.length })}
                  </span>
                </div>
                {ruta.descripcion && (
                  <p className="text-fluid-sm text-text-muted">{ruta.descripcion}</p>
                )}
                <ol className="mt-1 flex flex-wrap gap-x-2 gap-y-1 text-fluid-sm text-text-muted">
                  {ruta.paradas.map((parada, indice) => (
                    <li key={parada.lugarId}>
                      {indice > 0 && <span aria-hidden="true">→ </span>}
                      <Link
                        href={`/lugares/${parada.slug}`}
                        className="underline-offset-4 hover:underline"
                      >
                        {parada.nombre}
                      </Link>
                    </li>
                  ))}
                </ol>
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  );
}
