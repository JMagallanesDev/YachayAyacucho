"use client";

import { useFormatter, useLocale, useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { Link } from "@/i18n/navegacion";
import { obtenerPasaporte } from "@/lib/participacion";
import type { Pasaporte as DatosPasaporte } from "@/types/participacion";

/**
 * Pasaporte patrimonial (RF-39b).
 *
 * <p>Muestra tambien las insignias que faltan, con su descripcion: un pasaporte
 * que solo ensena lo conseguido no invita a seguir. Ver lo que queda y como
 * lograrlo es lo que hace que la gamificacion funcione.</p>
 */
export function Pasaporte() {
  const t = useTranslations("pasaporte");
  const formato = useFormatter();
  const idioma = useLocale();
  const { comprobando } = useSesionRequerida();

  const [datos, setDatos] = useState<DatosPasaporte | null>(null);
  const [compartido, setCompartido] = useState(false);

  const cargar = useCallback(() => obtenerPasaporte(idioma), [idioma]);

  useEffect(() => {
    if (!comprobando) {
      cargar().then(setDatos).catch(() => setDatos(null));
    }
  }, [comprobando, cargar]);

  /**
   * Diploma compartible.
   *
   * <p>Se compone el texto en el cliente y se ofrece a la API nativa de
   * compartir; si el navegador no la tiene —Safari de escritorio, Firefox— se
   * copia al portapapeles. No se genera ninguna pagina publica: eso expondria
   * el nombre y el progreso a cualquiera con el enlace, y para un diploma no
   * compensa.</p>
   */
  async function compartirDiploma() {
    if (!datos) return;

    const obtenidas = datos.insignias.filter((i) => i.obtenida);
    const texto = t("textoDiploma", {
      sellos: datos.sellos,
      total: datos.lugaresTotales,
      insignias: obtenidas.length,
    });

    try {
      if (typeof navigator !== "undefined" && navigator.share) {
        await navigator.share({ title: t("titulo"), text: texto });
      } else {
        await navigator.clipboard.writeText(texto);
      }
      setCompartido(true);
      setTimeout(() => setCompartido(false), 3000);
    } catch {
      // Cancelar el dialogo de compartir lanza: no es un error que mostrar.
    }
  }

  if (comprobando || !datos) {
    return null;
  }

  const porcentaje = datos.lugaresTotales > 0
    ? Math.round((datos.sellos / datos.lugaresTotales) * 100)
    : 0;

  return (
    <div className="flex flex-col gap-10" data-testid="pasaporte">
      {/* ---- Resumen ---------------------------------------------------- */}
      <section className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-6">
        <p className="text-fluid-3xl font-bold text-text" data-testid="total-sellos">
          {t("sellosDe", { sellos: datos.sellos, total: datos.lugaresTotales })}
        </p>

        <div
          className="h-2 w-full overflow-hidden rounded-full bg-surface-muted"
          role="progressbar"
          aria-valuenow={porcentaje}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={t("progresoGeneral")}
        >
          <div className="h-full rounded-full bg-primary" style={{ width: `${porcentaje}%` }} />
        </div>

        <button
          type="button"
          onClick={compartirDiploma}
          data-testid="compartir-diploma"
          className="press min-h-touch w-fit rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
        >
          {compartido ? t("copiado") : t("compartirDiploma")}
        </button>
      </section>

      {/* ---- Insignias -------------------------------------------------- */}
      <section className="flex flex-col gap-4">
        <h2 className="text-fluid-xl font-semibold text-text">{t("insignias")}</h2>
        <ul className="grid gap-3 sm:grid-cols-2" data-testid="lista-insignias">
          {datos.insignias.map((insignia) => (
            <li
              key={insignia.id}
              data-testid={`insignia-${insignia.codigo}`}
              data-obtenida={insignia.obtenida}
              className={`flex flex-col gap-1 rounded-card border p-4 ${
                insignia.obtenida
                  ? "border-accent bg-accent-subtle"
                  : "border-border-base bg-surface opacity-60"
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <strong className="text-fluid-base text-text">{insignia.nombre}</strong>
                {insignia.obtenida && insignia.obtenidaEn && (
                  <span className="text-fluid-sm text-text-muted">
                    {formato.dateTime(new Date(insignia.obtenidaEn), {
                      day: "numeric",
                      month: "short",
                    })}
                  </span>
                )}
              </div>
              {insignia.descripcion && (
                <p className="text-fluid-sm text-text-muted">{insignia.descripcion}</p>
              )}
            </li>
          ))}
        </ul>
      </section>

      {/* ---- Progreso por ruta ------------------------------------------ */}
      {datos.rutas.length > 0 && (
        <section className="flex flex-col gap-4">
          <h2 className="text-fluid-xl font-semibold text-text">{t("rutas")}</h2>
          <ul className="flex flex-col gap-4" data-testid="progreso-rutas">
            {datos.rutas.map((ruta) => (
              <li
                key={ruta.rutaId}
                data-testid={`ruta-${ruta.slug}`}
                data-completada={ruta.completada}
                className="flex flex-col gap-2"
              >
                <div className="flex items-baseline justify-between gap-3">
                  <span className="text-fluid-base font-medium text-text">{ruta.nombre}</span>
                  <span className="text-fluid-sm text-text-muted">
                    {t("paradasVisitadas", {
                      visitados: ruta.visitados,
                      total: ruta.total,
                    })}
                  </span>
                </div>
                <div
                  className="h-2 w-full overflow-hidden rounded-full bg-surface-muted"
                  role="progressbar"
                  aria-valuenow={ruta.total > 0 ? Math.round((ruta.visitados / ruta.total) * 100) : 0}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label={ruta.nombre}
                >
                  <div
                    className="h-full rounded-full"
                    style={{
                      width: `${ruta.total > 0 ? (ruta.visitados / ruta.total) * 100 : 0}%`,
                      backgroundColor: ruta.colorHex,
                    }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* ---- Sellos ------------------------------------------------------ */}
      <section className="flex flex-col gap-4">
        <h2 className="text-fluid-xl font-semibold text-text">{t("misVisitas")}</h2>

        {datos.visitas.length === 0 ? (
          <p data-testid="sin-sellos" className="text-fluid-sm text-text-muted">
            {t("sinVisitas")}
          </p>
        ) : (
          <ul className="grid gap-3 sm:grid-cols-2" data-testid="lista-sellos">
            {datos.visitas.map((sello) => (
              <li key={sello.lugarId}>
                <Link
                  href={`/lugares/${sello.slug}`}
                  data-testid="sello"
                  className="press flex items-center gap-3 rounded-card border border-border-base bg-surface p-3"
                >
                  <span
                    aria-hidden="true"
                    className="size-3 shrink-0 rounded-full"
                    style={{ backgroundColor: sello.colorCategoria }}
                  />
                  <span className="flex flex-col">
                    <span className="text-fluid-base text-text">{sello.nombre}</span>
                    <span className="text-fluid-sm text-text-muted">
                      {formato.dateTime(new Date(sello.visitadoEn), {
                        day: "numeric",
                        month: "long",
                        year: "numeric",
                      })}
                    </span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
