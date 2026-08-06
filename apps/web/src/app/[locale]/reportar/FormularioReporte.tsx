"use client";

import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { Link } from "@/i18n/navegacion";
import { CENTRO_HUAMANGA } from "@/lib/mapa";
import { crearReporte } from "@/lib/reportes";
import { useSesion } from "@/stores/sesion";
import type { NuevoReporte, TipoIncidente } from "@/types/reporte";

const MAXIMO_FOTOS = 5;

/**
 * Denuncia de un dano al patrimonio (RF-69 a RF-73).
 *
 * <p><strong>Cuatro decisiones para que se complete en menos de 60 segundos</strong>,
 * que es el requisito:</p>
 * <ul>
 *   <li>El tipo son <strong>botones grandes</strong>, no un desplegable: un
 *       toque en vez de abrir, buscar y elegir.</li>
 *   <li>La ubicacion se pide <strong>al abrir</strong>. Aqui si tiene sentido
 *       —a diferencia del resto de la aplicacion— porque es literalmente el
 *       proposito de la pantalla, y el pin queda ajustable si el GPS falla.</li>
 *   <li>Solo tres campos son obligatorios: tipo, descripcion y ubicacion.</li>
 *   <li><strong>Anonimo por defecto.</strong> Quien quiera dar su nombre lo
 *       activa; al reves, la gente descubriria tarde que quedo señalada.</li>
 * </ul>
 */
export function FormularioReporte({ tipos }: { tipos: TipoIncidente[] }) {
  const t = useTranslations("reportar");
  const idioma = useLocale();
  const usuario = useSesion((estado) => estado.usuario);
  const token = useSesion((estado) => estado.accessToken);

  const [tipoId, setTipoId] = useState<string | null>(null);
  const [descripcion, setDescripcion] = useState("");
  const [referencia, setReferencia] = useState("");
  // Se arranca ya con el centro de Huamanga en vez de con null. Asi el
  // formulario esta completo desde el primer fotograma —el GPS solo lo
  // corrige— y nunca hay un estado en el que falte la ubicacion.
  const [posicion, setPosicion] = useState({
    lon: CENTRO_HUAMANGA.longitud,
    lat: CENTRO_HUAMANGA.latitud,
  });
  // Arranca siempre en "buscando", nunca mirando `navigator`: el servidor no
  // tiene ese objeto y renderizaria "ajusta el pin" mientras el navegador
  // renderiza "buscando", que es exactamente una discordancia de hidratacion.
  // Si no hay geolocalizacion, el efecto lo corrige en el primer fotograma.
  const [estadoGps, setEstadoGps] = useState<"buscando" | "listo" | "manual">("buscando");
  const [fotos, setFotos] = useState<File[]>([]);
  const [anonimo, setAnonimo] = useState(true);
  const [nombre, setNombre] = useState("");

  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enviado, setEnviado] = useState(false);

  // La ubicacion se pide al montar. Si no llega, se cae al centro de Huamanga
  // y se avisa de que hay que ajustar el pin: es mejor un punto aproximado y
  // corregible que bloquear la denuncia.
  useEffect(() => {
    // Permiso denegado, sin senal o navegador sin geolocalizacion: se queda el
    // centro de Huamanga y se pide ajustar el pin. Bloquear la denuncia por
    // esto seria absurdo. Los tres casos acaban en el mismo sitio.
    const sinUbicacion = () => setEstadoGps("manual");

    if (!navigator.geolocation) {
      // Se aplaza para que sea una respuesta asincrona mas, igual que el
      // callback de error, y no un cambio de estado dentro del efecto.
      queueMicrotask(sinUbicacion);
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (p) => {
        setPosicion({ lon: p.coords.longitude, lat: p.coords.latitude });
        setEstadoGps("listo");
      },
      sinUbicacion,
      { enableHighAccuracy: true, timeout: 10000 },
    );
  }, []);

  function elegirFotos(evento: React.ChangeEvent<HTMLInputElement>) {
    const elegidas = Array.from(evento.target.files ?? []).slice(0, MAXIMO_FOTOS);
    setFotos(elegidas);
  }

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    if (!tipoId) return;

    setEnviando(true);
    setError(null);

    const datos: NuevoReporte = {
      tipoIncidenteId: tipoId,
      descripcion: descripcion.trim(),
      longitud: posicion.lon,
      latitud: posicion.lat,
      esAnonimo: anonimo,
    };
    if (referencia.trim()) {
      datos.direccionReferencial = referencia.trim();
    }
    if (!anonimo && nombre.trim()) {
      datos.nombreReportante = nombre.trim();
    }

    try {
      await crearReporte(datos, fotos, idioma, anonimo ? null : token);
      setEnviado(true);
    } catch (fallo) {
      setError(fallo instanceof Error ? fallo.message : t("errorGenerico"));
    } finally {
      setEnviando(false);
    }
  }

  if (enviado) {
    return (
      <div
        role="status"
        data-testid="reporte-enviado"
        className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-6"
      >
        <strong className="text-fluid-xl text-text">{t("gracias")}</strong>
        <p className="text-fluid-base text-text-muted">{t("enRevision")}</p>
        <Link
          href="/mapa-incidentes"
          className="press min-h-touch w-fit rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
        >
          {t("verMapa")}
        </Link>
      </div>
    );
  }

  const listoParaEnviar = tipoId !== null && descripcion.trim().length > 0;

  return (
    <form onSubmit={enviar} className="flex flex-col gap-8" data-testid="formulario-reporte">
      {/* ---- 1. Tipo: un toque (RF-70) -------------------------------- */}
      <fieldset className="flex flex-col gap-3">
        <legend className="text-fluid-lg font-semibold text-text">{t("queViste")}</legend>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {tipos.map((tipo) => (
            <button
              key={tipo.id}
              type="button"
              onClick={() => setTipoId(tipo.id)}
              aria-pressed={tipoId === tipo.id}
              data-testid={`tipo-${tipo.codigo}`}
              className="press min-h-touch rounded-card border-2 p-3 text-fluid-sm font-medium transition-colors"
              style={{
                borderColor: tipoId === tipo.id ? tipo.colorHex : "transparent",
                backgroundColor: tipoId === tipo.id ? `${tipo.colorHex}1a` : "var(--color-surface-muted)",
                color: tipoId === tipo.id ? tipo.colorHex : "var(--color-text)",
              }}
            >
              {tipo.nombre}
            </button>
          ))}
        </div>
      </fieldset>

      {/* ---- 2. Descripcion -------------------------------------------- */}
      <label className="flex flex-col gap-2">
        <span className="text-fluid-lg font-semibold text-text">{t("cuentaQuePasa")}</span>
        <textarea
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          maxLength={2000}
          rows={4}
          required
          placeholder={t("descripcionPlaceholder")}
          data-testid="descripcion-reporte"
          className="rounded-card border border-border-base bg-surface p-3 text-text"
        />
      </label>

      {/* ---- 3. Ubicacion ajustable (RF-71) ---------------------------- */}
      <fieldset className="flex flex-col gap-2">
        <legend className="text-fluid-lg font-semibold text-text">{t("donde")}</legend>

        <p className="text-fluid-sm text-text-muted" data-testid="estado-gps">
          {estadoGps === "buscando"
            ? t("buscandoUbicacion")
            : estadoGps === "manual"
              ? t("ajustaElPin")
              : t("ubicacionDetectada")}
        </p>

        <div className="flex flex-wrap gap-3">
            <label className="flex flex-col gap-1 text-fluid-sm text-text-muted">
              {t("latitud")}
              <input
                type="number"
                step="0.0001"
                value={posicion.lat}
                onChange={(e) => setPosicion({ ...posicion, lat: Number(e.target.value) })}
                data-testid="latitud-reporte"
                className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
              />
            </label>
            <label className="flex flex-col gap-1 text-fluid-sm text-text-muted">
              {t("longitud")}
              <input
                type="number"
                step="0.0001"
                value={posicion.lon}
                onChange={(e) => setPosicion({ ...posicion, lon: Number(e.target.value) })}
                data-testid="longitud-reporte"
                className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
              />
            </label>
        </div>

        <label className="flex flex-col gap-1">
          <span className="text-fluid-sm text-text-muted">{t("referencia")}</span>
          <input
            type="text"
            value={referencia}
            onChange={(e) => setReferencia(e.target.value)}
            maxLength={255}
            placeholder={t("referenciaPlaceholder")}
            data-testid="referencia-reporte"
            className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
          />
        </label>
      </fieldset>

      {/* ---- 4. Fotos opcionales (RF-73) ------------------------------- */}
      <label className="flex flex-col gap-2">
        <span className="text-fluid-lg font-semibold text-text">{t("fotos")}</span>
        <span className="text-fluid-sm text-text-muted">{t("fotosAyuda")}</span>
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          onChange={elegirFotos}
          data-testid="fotos-reporte"
          className="text-fluid-sm text-text"
        />
        {fotos.length > 0 && (
          <span className="text-fluid-sm text-text-muted">
            {t("fotosElegidas", { total: fotos.length })}
          </span>
        )}
      </label>

      {/* ---- 5. Anonimato (RF-72) -------------------------------------- */}
      <fieldset className="flex flex-col gap-3 rounded-card bg-surface-muted p-4">
        <legend className="sr-only">{t("identidad")}</legend>

        <label className="flex items-start gap-3">
          <input
            type="checkbox"
            checked={anonimo}
            onChange={(e) => setAnonimo(e.target.checked)}
            data-testid="casilla-anonimo"
            className="mt-1 size-5"
          />
          <span className="flex flex-col gap-1">
            <span className="text-fluid-base font-medium text-text">{t("reportarAnonimo")}</span>
            <span className="text-fluid-sm text-text-muted">{t("explicacionAnonimo")}</span>
          </span>
        </label>

        {/* El precio del anonimato se dice ANTES de enviar, no despues: quien
            elige que no le atribuyan nada debe saber que renuncia al sello. */}
        {anonimo && usuario && (
          <p data-testid="aviso-insignia" className="text-fluid-sm text-text-muted">
            {t("avisoInsignia")}
          </p>
        )}

        {!anonimo && (
          <label className="flex flex-col gap-1">
            <span className="text-fluid-sm text-text-muted">{t("tuNombre")}</span>
            <input
              type="text"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              maxLength={120}
              data-testid="nombre-reportante"
              className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
            />
          </label>
        )}
      </fieldset>

      {error && (
        <p role="alert" data-testid="error-reporte" className="rounded-card bg-danger-subtle p-4 text-text">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={!listoParaEnviar || enviando}
        data-testid="enviar-reporte"
        className="press min-h-touch rounded-card bg-primary px-6 py-3 text-fluid-base font-medium text-primary-fg disabled:opacity-50"
      >
        {enviando ? t("enviando") : t("enviarReporte")}
      </button>
    </form>
  );
}
