import { getTranslations, setRequestLocale } from "next-intl/server";

import { ProximosEventos } from "@/components/agenda/ProximosEventos";
import { obtenerSalud } from "@/lib/api";
import { env } from "@/lib/env";
import type { ComponenteSalud } from "@/types/health";

/**
 * Portada provisional.
 *
 * <p>Es un Server Component: el estado se consulta en el servidor y llega al
 * navegador como HTML ya renderizado, sin JavaScript de cliente.</p>
 *
 * <p>Desde el Bloque 3 todos sus textos salen de next-intl: cero cadenas
 * escritas en el componente (RF-66). La portada real, con el listado de
 * lugares, llega en el Bloque 4.</p>
 */

const NOMBRES_COMPONENTE: Record<string, string> = {
  postgresql: "PostgreSQL 16 + PostGIS",
  redis: "Redis",
};

const PALETA = [
  { clave: "retablo", clase: "bg-retablo-600" },
  { clave: "anil", clase: "bg-anil-800" },
  { clave: "quinua", clase: "bg-quinua-500" },
  { clave: "sillar", clase: "bg-sillar-200" },
  { clave: "puna", clase: "bg-puna-600" },
] as const;

export default async function PaginaInicio({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);

  const t = await getTranslations("portada");
  const tc = await getTranslations("colores");
  const resultado = await obtenerSalud();

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-8 px-5 py-12">
      <header className="flex flex-col gap-3">
        <span className="text-fluid-sm font-medium uppercase tracking-widest text-accent">
          {t("etiqueta")}
        </span>
        <h1 className="text-fluid-3xl font-bold text-text">{env.appName}</h1>
        <p className="text-fluid-base text-text-muted">{t("descripcion")}</p>
      </header>

      <section
        aria-labelledby="titulo-estado"
        className="rounded-card border border-border-base bg-surface p-6 shadow-card"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 id="titulo-estado" className="text-fluid-lg font-semibold text-text">
            {t("estadoSistema")}
          </h2>
          <EtiquetaEstado
            ok={resultado.alcanzable && resultado.salud.status === "UP"}
            texto={
              !resultado.alcanzable
                ? t("sinConexion")
                : resultado.salud.status === "UP"
                  ? t("operativo")
                  : t("degradado")
            }
          />
        </div>

        {resultado.alcanzable ? (
          <ul className="mt-5 flex flex-col divide-y divide-border-base">
            <FilaComponente
              nombre={t("apiSpring")}
              detalle={t("aplicacion", { nombre: resultado.salud.application })}
              ok
              milisegundos={null}
            />
            {resultado.salud.components.map((componente: ComponenteSalud) => (
              <FilaComponente
                key={componente.name}
                nombre={NOMBRES_COMPONENTE[componente.name] ?? componente.name}
                detalle={componente.detail}
                ok={componente.status === "UP"}
                milisegundos={componente.responseTimeMs}
              />
            ))}
          </ul>
        ) : (
          <div className="mt-5 rounded-card bg-danger-subtle p-4">
            <p className="font-medium text-text">{resultado.motivo}</p>
            <p className="mt-1 text-fluid-sm text-text-muted">
              {t("sinConexionAyuda", { url: env.apiUrl })}
            </p>
          </div>
        )}
      </section>

      {/* Proximos eventos con cuenta regresiva (RF-84). Se coloca sobre la
          paleta porque es contenido para el visitante; lo de abajo es
          diagnostico del sistema y desaparecera con la portada real. */}
      <ProximosEventos idioma={locale} />

      <section aria-labelledby="titulo-paleta" className="flex flex-col gap-4">
        <h2 id="titulo-paleta" className="text-fluid-lg font-semibold text-text">
          {t("paleta")}
        </h2>
        {/* Unico lugar donde se usan colores primitivos directamente: este
            bloque existe justamente para mostrarlos. El resto de la
            aplicacion consume solo tokens semanticos. */}
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-5">
          {PALETA.map((color) => (
            <li key={color.clave} className="flex flex-col gap-2">
              <span
                className={`h-14 w-full rounded-card border border-border-base ${color.clase}`}
                aria-hidden="true"
              />
              <span className="text-fluid-sm font-medium text-text">{tc(color.clave)}</span>
              <span className="text-fluid-sm text-text-muted">{tc(`${color.clave}Origen`)}</span>
            </li>
          ))}
        </ul>
      </section>

      <footer className="mt-auto border-t border-border-base pt-6 text-fluid-sm text-text-muted">
        {t("tesis")}
      </footer>
    </main>
  );
}

function EtiquetaEstado({ ok, texto }: { ok: boolean; texto: string }) {
  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-fluid-sm font-medium ${
        ok ? "bg-success-subtle text-success" : "bg-danger-subtle text-danger"
      }`}
    >
      <span className={`size-2 rounded-full ${ok ? "bg-success" : "bg-danger"}`} aria-hidden="true" />
      {texto}
    </span>
  );
}

function FilaComponente({
  nombre,
  detalle,
  ok,
  milisegundos,
}: {
  nombre: string;
  detalle: string;
  ok: boolean;
  milisegundos: number | null;
}) {
  return (
    <li className="flex items-center justify-between gap-4 py-3">
      <span className="flex flex-col">
        <span className="font-medium text-text">{nombre}</span>
        <span className="text-fluid-sm text-text-muted">{detalle}</span>
      </span>
      <span className="flex shrink-0 items-center gap-3">
        {milisegundos !== null && (
          <span className="text-fluid-sm tabular-nums text-text-muted">{milisegundos} ms</span>
        )}
        <span
          className={`size-2.5 rounded-full ${ok ? "bg-success" : "bg-danger"}`}
          role="img"
          aria-label={ok ? "OK" : "ERROR"}
        />
      </span>
    </li>
  );
}
