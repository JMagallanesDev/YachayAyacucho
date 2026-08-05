"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";

import { BotonUbicacion } from "@/components/lugares/BotonUbicacion";
import { EsqueletoLugares } from "@/components/lugares/EsqueletoLugares";
import { LugarCard } from "@/components/lugares/LugarCard";
import { useDebounce } from "@/hooks/useDebounce";
import { usePathname, useRouter } from "@/i18n/navegacion";
import { buscarLugares, claveLugares } from "@/lib/lugares";
import type { Categoria, CriteriosLugares, OrdenLugares } from "@/types/lugar";

const ORDENES: OrdenLugares[] = ["ALFABETICO", "MEJOR_VALORADOS", "MAS_VISITADOS"];

/**
 * Explorador de lugares: busqueda, filtros y paginacion
 * (RF-01, RF-02, RF-04, RF-05, RF-06).
 *
 * <p>Es el unico Client Component de la pagina. El Server Component que lo
 * envuelve ya dejo la primera consulta en la cache de TanStack Query, asi que
 * en la primera pintura los datos ya estan: no hay parpadeo de carga aunque
 * este componente sea de cliente.</p>
 *
 * <p><strong>Los criterios viven en la URL</strong>, no en estado local. Asi
 * una busqueda se puede compartir, el boton atras del navegador funciona y
 * recargar no pierde lo que el usuario habia filtrado.</p>
 */
export function ExploradorLugares({ categorias }: { categorias: Categoria[] }) {
  const t = useTranslations("lugares");
  const idioma = useLocale();
  const router = useRouter();
  const ruta = usePathname();
  const parametros = useSearchParams();

  // Lo que se teclea se refleja al instante en el input; lo que dispara la
  // consulta es la version con retardo.
  const [texto, setTexto] = useState(parametros.get("q") ?? "");
  const textoConRetardo = useDebounce(texto, 300);

  const criterios: CriteriosLugares = {
    q: textoConRetardo || undefined,
    categoriaId: parametros.get("categoria") ?? undefined,
    orden: (parametros.get("orden") as OrdenLugares) ?? undefined,
    pagina: Number(parametros.get("pagina") ?? 0),
  };

  // La URL se sincroniza con el texto ya estabilizado, no con cada tecla:
  // escribir "catedral" dejaria ocho entradas en el historial.
  useEffect(() => {
    const actuales = new URLSearchParams(parametros.toString());
    const anterior = actuales.get("q") ?? "";
    if (anterior === textoConRetardo) {
      return;
    }
    if (textoConRetardo) {
      actuales.set("q", textoConRetardo);
    } else {
      actuales.delete("q");
    }
    actuales.delete("pagina");
    router.replace(`${ruta}?${actuales}`, { scroll: false });
  }, [textoConRetardo, parametros, router, ruta]);

  const { data, isPending, isFetching } = useQuery({
    queryKey: claveLugares(criterios, idioma),
    queryFn: () => buscarLugares(criterios, idioma),
    // Mantiene visible la lista anterior mientras llega la nueva. Sin esto,
    // cada pulsacion vaciaria la pantalla y la busqueda parpadearia.
    placeholderData: keepPreviousData,
  });

  function actualizar(clave: string, valor: string | null) {
    const actuales = new URLSearchParams(parametros.toString());
    if (valor) {
      actuales.set(clave, valor);
    } else {
      actuales.delete(clave);
    }
    // Cambiar un filtro siempre devuelve a la primera pagina: quedarse en la
    // pagina 3 de un resultado que ahora tiene una sola es desconcertante.
    if (clave !== "pagina") {
      actuales.delete("pagina");
    }
    router.replace(`${ruta}?${actuales}`, { scroll: false });
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-4">
        <label htmlFor="buscador" className="sr-only">
          {t("buscar")}
        </label>
        <input
          id="buscador"
          type="search"
          inputMode="search"
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          placeholder={t("buscarPlaceholder")}
          data-testid="buscador-lugares"
          className="min-h-touch rounded-card border border-border-base bg-surface px-4 text-text"
        />

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => actualizar("categoria", null)}
            aria-pressed={!criterios.categoriaId}
            className={`press min-h-touch rounded-card px-3 text-fluid-sm font-medium ${
              !criterios.categoriaId ? "bg-primary text-primary-fg" : "bg-surface-muted text-text-muted"
            }`}
          >
            {t("todas")}
          </button>
          {categorias.map((categoria) => (
            <button
              key={categoria.id}
              type="button"
              onClick={() => actualizar("categoria", categoria.id)}
              aria-pressed={criterios.categoriaId === categoria.id}
              data-testid={`filtro-${categoria.codigo}`}
              className={`press min-h-touch rounded-card px-3 text-fluid-sm font-medium ${
                criterios.categoriaId === categoria.id
                  ? "bg-primary text-primary-fg"
                  : "bg-surface-muted text-text-muted"
              }`}
            >
              {categoria.nombre}
            </button>
          ))}
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <label htmlFor="orden" className="text-fluid-sm text-text-muted">
              {t("ordenar")}
            </label>
            <select
              id="orden"
              value={criterios.orden ?? "ALFABETICO"}
              onChange={(evento) => actualizar("orden", evento.target.value)}
              data-testid="selector-orden"
              className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-fluid-sm text-text"
            >
              {ORDENES.map((orden) => (
                <option key={orden} value={orden}>
                  {t(`orden${orden}`)}
                </option>
              ))}
            </select>
          </div>

          <BotonUbicacion />
        </div>
      </div>

      {isPending ? (
        <EsqueletoLugares />
      ) : !data || data.content.length === 0 ? (
        <p data-testid="sin-resultados" className="rounded-card bg-surface-muted p-6 text-text-muted">
          {t("sinResultados")}
        </p>
      ) : (
        <>
          <p aria-live="polite" className="text-fluid-sm text-text-muted">
            {t("resultados", { total: data.totalElements })}
          </p>

          <ul
            data-testid="lista-lugares"
            // Se atenua mientras llega una busqueda nueva, en vez de vaciarse.
            className={`grid gap-4 transition-opacity sm:grid-cols-2 lg:grid-cols-3 ${
              isFetching ? "opacity-60" : "opacity-100"
            }`}
          >
            {data.content.map((lugar) => (
              <li key={lugar.id}>
                <LugarCard lugar={lugar} />
              </li>
            ))}
          </ul>

          {data.totalPages > 1 && (
            <nav aria-label={t("paginacion")} className="flex items-center justify-center gap-3">
              <button
                type="button"
                disabled={data.first}
                onClick={() => actualizar("pagina", String(data.number - 1))}
                className="press min-h-touch rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text disabled:opacity-40"
              >
                {t("anterior")}
              </button>
              <span className="text-fluid-sm text-text-muted">
                {t("paginaDe", { actual: data.number + 1, total: data.totalPages })}
              </span>
              <button
                type="button"
                disabled={data.last}
                onClick={() => actualizar("pagina", String(data.number + 1))}
                data-testid="pagina-siguiente"
                className="press min-h-touch rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text disabled:opacity-40"
              >
                {t("siguiente")}
              </button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}
