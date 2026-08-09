import { getTranslations, setRequestLocale } from "next-intl/server";
import { notFound } from "next/navigation";

import { Compartir } from "@/components/Compartir";
import { Link } from "@/i18n/navegacion";
import { negocioPorId } from "@/lib/negocios";

import { BotonesDeContacto } from "./BotonesDeContacto";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string; id: string }>;
}) {
  const { locale, id } = await params;
  const negocio = await negocioPorId(id, locale);
  return negocio
    ? { title: negocio.nombre, description: negocio.descripcion ?? undefined }
    : {};
}

/**
 * Ficha de un negocio del directorio (RF-105, RF-110).
 *
 * <p>Es dinamica y no ISR a proposito: <strong>abrir la ficha cuenta como una
 * visita</strong> en la analitica del negocio, y una pagina servida desde la
 * cache no llegaria al backend y no se contaria. El throttling de 30 minutos ya
 * evita que recargar infle el numero, asi que la cache no hace falta para
 * proteger el contador.</p>
 *
 * <p>Un negocio no aprobado devuelve 404 aqui: el backend solo entrega los
 * APROBADOS.</p>
 */
export const dynamic = "force-dynamic";

export default async function PaginaNegocio({
  params,
}: {
  params: Promise<{ locale: string; id: string }>;
}) {
  const { locale, id } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("negocios");
  const negocio = await negocioPorId(id, locale);

  if (!negocio) {
    notFound();
  }

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-6 px-5 py-10">
      <Link
        href="/negocios"
        className="press w-fit text-fluid-sm font-medium text-text-muted underline-offset-4 hover:underline"
      >
        {t("volverAlDirectorio")}
      </Link>

      <header className="flex flex-col gap-3">
        <span className="w-fit rounded-full bg-surface-muted px-2.5 py-1 text-fluid-sm text-text-muted">
          {negocio.categoriaNombre}
        </span>
        <h1 className="text-fluid-2xl font-bold text-text" data-testid="nombre-negocio">
          {negocio.nombre}
        </h1>
        <p className="text-fluid-sm text-text-muted">{negocio.distritoNombre}</p>
      </header>

      {negocio.descripcion && (
        <p className="whitespace-pre-line text-fluid-base leading-relaxed text-text">
          {negocio.descripcion}
        </p>
      )}

      <BotonesDeContacto negocio={negocio} />

      <dl className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-5">
        {negocio.direccion && (
          <div className="flex flex-col gap-0.5">
            <dt className="text-fluid-sm text-text-muted">{t("direccion")}</dt>
            <dd className="text-fluid-base text-text">{negocio.direccion}</dd>
          </div>
        )}
        {negocio.horarioTexto && (
          <div className="flex flex-col gap-0.5">
            <dt className="text-fluid-sm text-text-muted">{t("horario")}</dt>
            <dd className="text-fluid-base text-text">{negocio.horarioTexto}</dd>
          </div>
        )}
        {negocio.telefono && (
          <div className="flex flex-col gap-0.5">
            <dt className="text-fluid-sm text-text-muted">{t("telefono")}</dt>
            <dd className="text-fluid-base text-text">{negocio.telefono}</dd>
          </div>
        )}
      </dl>

      <Compartir titulo={negocio.nombre} />
    </main>
  );
}
