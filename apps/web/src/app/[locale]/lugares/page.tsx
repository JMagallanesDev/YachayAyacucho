import { HydrationBoundary, QueryClient, dehydrate } from "@tanstack/react-query";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Suspense } from "react";

import { EsqueletoLugares } from "@/components/lugares/EsqueletoLugares";
import { env } from "@/lib/env";
import { buscarLugares, claveLugares } from "@/lib/lugares";
import type { Categoria } from "@/types/lugar";

import { ExploradorLugares } from "./ExploradorLugares";

/**
 * Listado de lugares patrimoniales (RF-01).
 *
 * <p>Server Component con ISR: el HTML se pre-genera y se sirve desde el CDN
 * sin tocar la base de datos. Antes de entregarlo hace <em>prefetch</em> de la
 * primera consulta y la deja en la cache de TanStack Query, de modo que el
 * componente de cliente ya tiene datos en la primera pintura y no aparece
 * ningun esqueleto en la carga inicial.</p>
 */
export const revalidate = 300;

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "lugares" });
  return { title: t("titulo"), description: t("descripcion") };
}

async function obtenerCategorias(idioma: string): Promise<Categoria[]> {
  try {
    const respuesta = await fetch(`${env.apiUrl}/categorias?idioma=${idioma.toUpperCase()}`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 3600 },
    });
    return respuesta.ok ? respuesta.json() : [];
  } catch {
    // Sin categorias el listado sigue siendo util: solo se queda sin filtros.
    return [];
  }
}

export default async function PaginaLugares({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("lugares");

  // Un QueryClient por peticion. Uno global en el modulo se compartiria entre
  // visitantes distintos, mezclando sus datos en la misma cache.
  const clienteQuery = new QueryClient();
  const criteriosIniciales = { pagina: 0 };

  const [categorias] = await Promise.all([
    obtenerCategorias(locale),
    clienteQuery.prefetchQuery({
      queryKey: claveLugares(criteriosIniciales, locale),
      queryFn: () => buscarLugares(criteriosIniciales, locale),
    }),
  ]);

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-5xl flex-col gap-8 px-5 py-10">
      <header className="flex flex-col gap-2">
        <h1 className="text-fluid-3xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
      </header>

      <HydrationBoundary state={dehydrate(clienteQuery)}>
        {/* El explorador lee los filtros de la URL con useSearchParams, y eso
            solo se conoce al servir la peticion. Sin este limite de Suspense
            Next no puede pre-generar la pagina y el build falla: los criterios
            de busqueda no existen todavia cuando se compila. Con el, el
            esqueleto de la lista es lo que se pre-genera y se cachea, y los
            filtros se resuelven al llegar la peticion. */}
        <Suspense fallback={<EsqueletoLugares />}>
          <ExploradorLugares categorias={categorias} />
        </Suspense>
      </HydrationBoundary>
    </main>
  );
}
