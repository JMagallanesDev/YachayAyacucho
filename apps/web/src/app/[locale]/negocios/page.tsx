import { getTranslations, setRequestLocale } from "next-intl/server";

import { RegistrarVisita } from "@/components/RegistrarVisita";
import { Link } from "@/i18n/navegacion";
import { categoriasDeNegocio, directorio } from "@/lib/negocios";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "negocios" });
  return { title: t("titulo"), description: t("descripcion") };
}

/**
 * Directorio de negocios locales (RF-105).
 *
 * <p>Solo lista los <strong>aprobados</strong>, y esa garantia no vive aqui:
 * el estado esta escrito dentro de la consulta del backend, no llega como
 * parametro. Esta pagina no tiene forma de pedir un negocio pendiente aunque
 * alguien manipule la URL.</p>
 *
 * <p>El filtro por categoria va en la URL, igual que los del Bloque 4 y los de
 * la agenda: enlaces que funcionan sin JavaScript y se pueden compartir.</p>
 */
export const revalidate = 300;

export default async function PaginaNegocios({
  params,
  searchParams,
}: {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ categoria?: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("negocios");
  const filtros = await searchParams;

  const [categorias, pagina] = await Promise.all([
    categoriasDeNegocio(locale),
    directorio(locale, filtros.categoria),
  ]);

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-3xl flex-col gap-6 px-5 py-10">
      <RegistrarVisita tipo="DIRECTORIO" />

      <header className="flex flex-col gap-3">
        <h1 className="text-fluid-2xl font-bold text-text">{t("titulo")}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
        <Link
          href="/negocios/registrar"
          className="press min-h-touch w-fit rounded-card bg-primary px-5 py-2 text-fluid-sm font-medium text-primary-fg"
        >
          {t("registrarMiNegocio")}
        </Link>
      </header>

      <div className="flex flex-wrap gap-2" data-testid="filtros-categoria">
        <Link
          href="/negocios"
          aria-current={filtros.categoria ? undefined : "true"}
          className={`press min-h-touch rounded-card px-3 py-1.5 text-fluid-sm font-medium ${
            filtros.categoria ? "bg-surface-muted text-text-muted" : "bg-primary text-primary-fg"
          }`}
        >
          {t("todas")}
        </Link>
        {categorias.map((categoria) => (
          <Link
            key={categoria.id}
            href={`/negocios?categoria=${categoria.id}`}
            aria-current={filtros.categoria === categoria.id ? "true" : undefined}
            data-testid={`filtro-${categoria.codigo}`}
            className={`press min-h-touch rounded-card px-3 py-1.5 text-fluid-sm font-medium ${
              filtros.categoria === categoria.id
                ? "bg-primary text-primary-fg"
                : "bg-surface-muted text-text-muted"
            }`}
          >
            {categoria.nombre}
          </Link>
        ))}
      </div>

      <h2 className="text-fluid-xl font-semibold text-text">
        {t("totalNegocios", { total: pagina.totalElements })}
      </h2>

      {pagina.content.length === 0 ? (
        <p data-testid="sin-negocios" className="rounded-card bg-surface-muted p-6 text-text-muted">
          {t("sinNegocios")}
        </p>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2" data-testid="lista-negocios">
          {pagina.content.map((negocio) => (
            <li
              key={negocio.id}
              data-testid="tarjeta-negocio"
              data-negocio-id={negocio.id}
              data-estado={negocio.estado}
              className="flex flex-col gap-2 rounded-card border border-border-base bg-surface p-4 shadow-card"
            >
              <span className="w-fit rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted">
                {negocio.categoriaNombre}
              </span>

              <Link
                href={`/negocios/${negocio.id}`}
                className="press text-fluid-lg font-semibold text-text underline-offset-4 hover:underline"
              >
                {negocio.nombre}
              </Link>

              {negocio.descripcion && (
                <p className="line-clamp-3 text-fluid-sm text-text-muted">{negocio.descripcion}</p>
              )}

              <span className="text-fluid-sm text-text-muted">{negocio.distritoNombre}</span>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
