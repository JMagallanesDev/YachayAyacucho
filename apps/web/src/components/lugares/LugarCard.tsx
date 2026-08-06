"use client";

import { useFormatter, useTranslations } from "next-intl";

import { BadgeApertura } from "@/components/lugares/BadgeApertura";
import { BotonFavorito } from "@/components/participacion/BotonFavorito";
import { Link } from "@/i18n/navegacion";
import { distanciaKm, proximidad } from "@/lib/geo";
import { useUbicacion } from "@/stores/ubicacion";
import type { LugarResumen } from "@/types/lugar";

/**
 * Tarjeta de un lugar en el listado (RF-01).
 *
 * <p>Muestra el badge abierto/cerrado (RF-09b) y, si el visitante concedio su
 * ubicacion, a cuantos minutos a pie queda (RF-09c). La distancia se calcula
 * aqui mismo con las coordenadas que ya vienen en la respuesta: ni una
 * llamada extra, y funciona sobre paginas cacheadas porque el HTML no depende
 * de donde este nadie.</p>
 */
export function LugarCard({ lugar }: { lugar: LugarResumen }) {
  const t = useTranslations("lugares");
  const formato = useFormatter();
  const lat = useUbicacion((estado) => estado.lat);
  const lon = useUbicacion((estado) => estado.lon);

  const cerca =
    lat !== null && lon !== null
      ? proximidad(distanciaKm({ lat, lon }, { lat: lugar.latitud, lon: lugar.longitud }))
      : null;

  const precio = Number(lugar.precioEntradaPen ?? 0);
  const calificacion = Number(lugar.calificacionPromedio ?? 0);

  return (
    <Link
      href={`/lugares/${lugar.slug}`}
      data-testid="tarjeta-lugar"
      data-slug={lugar.slug}
      className="press flex flex-col gap-3 rounded-card border border-border-base bg-surface p-5 shadow-card"
    >
      <div className="flex flex-wrap items-center gap-2">
        <span
          className="rounded-full px-2.5 py-1 text-fluid-sm font-medium"
          style={{ backgroundColor: `${lugar.categoria.colorHex}1a`, color: lugar.categoria.colorHex }}
        >
          {lugar.categoria.nombre}
        </span>
        <BadgeApertura horarios={lugar.horarios} />
        <span className="ml-auto">
          <BotonFavorito slug={lugar.slug} compacto />
        </span>
      </div>

      <h3 className="text-fluid-lg font-semibold text-text">{lugar.nombre}</h3>

      {lugar.descripcion && (
        <p className="line-clamp-2 text-fluid-sm text-text-muted">{lugar.descripcion}</p>
      )}

      <dl className="mt-auto flex flex-wrap items-center gap-x-4 gap-y-1 text-fluid-sm text-text-muted">
        <div className="flex items-center gap-1">
          <dt className="sr-only">{t("precio")}</dt>
          <dd className="font-medium text-text">
            {precio > 0 ? formato.number(precio, { style: "currency", currency: "PEN" }) : t("gratis")}
          </dd>
        </div>

        {lugar.duracionVisitaMin && (
          <div className="flex items-center gap-1">
            <dt className="sr-only">{t("duracion")}</dt>
            <dd>{t("minutos", { minutos: lugar.duracionVisitaMin })}</dd>
          </div>
        )}

        {cerca !== null && (
          <div className="flex items-center gap-1" data-testid="distancia-caminando">
            <dt className="sr-only">{t("distancia")}</dt>
            <dd className="font-medium text-secondary">
              {cerca.tipo === "caminando"
                ? t("aMinutosCaminando", { minutos: cerca.minutos })
                : t("aKilometros", { km: cerca.km })}
            </dd>
          </div>
        )}

        {calificacion > 0 && (
          <div className="flex items-center gap-1">
            <dt className="sr-only">{t("calificacion")}</dt>
            <dd className="font-medium text-accent">
              {formato.number(calificacion, { maximumFractionDigits: 1 })} ★
              <span className="ml-1 font-normal text-text-muted">({lugar.totalResenas})</span>
            </dd>
          </div>
        )}
      </dl>
    </Link>
  );
}
