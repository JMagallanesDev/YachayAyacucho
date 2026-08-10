"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";

import { useValorDelCliente } from "@/components/useValorDelCliente";

type Estado = "cerrado" | "abierto";

/**
 * Compartir un lugar por URL, QR o la hoja nativa del sistema (RF-15).
 *
 * <p><strong>Tres decisiones.</strong></p>
 *
 * <ol>
 *   <li><strong>El QR se genera en el navegador.</strong> Los generadores por
 *       URL que devuelven una imagen son comodos, pero significan mandarle a un
 *       tercero cada direccion que alguien comparte y desde donde. Para una
 *       aplicacion de patrimonio publico eso no compensa la comodidad.</li>
 *   <li><strong>La libreria se carga solo al abrir el panel</strong>, con un
 *       import dinamico. Asi no pesa en ninguna ficha para la inmensa mayoria
 *       que nunca pulsa compartir.</li>
 *   <li><strong>`navigator.share` es mejora progresiva.</strong> Existe en
 *       moviles y casi en ningun escritorio, asi que el boton solo se pinta
 *       cuando la API esta; no se ofrece algo que va a fallar.</li>
 * </ol>
 */
export function Compartir({ titulo }: { titulo: string }) {
  const t = useTranslations("compartir");
  const [estado, setEstado] = useState<Estado>("cerrado");
  const [copiado, setCopiado] = useState(false);
  // `navigator` y `window` no existen en el servidor. `useValorDelCliente`
  // —el hook del Bloque 9 sobre useSyncExternalStore— resuelve las dos: el
  // render del servidor y el primer render del navegador usan la instantanea
  // del servidor, asi que coinciden byte a byte, y el valor real entra al
  // terminar la hidratacion. Leerlo durante el render seria una discordancia;
  // leerlo en un efecto encadenaria un render de mas.
  const hayShareNativo = useValorDelCliente(
    () => typeof navigator.share === "function",
    () => false,
  );
  const url = useValorDelCliente(
    () => window.location.href,
    () => "",
  );
  const [urlQr, setUrlQr] = useState<string | null>(null);

  async function abrir() {
    setEstado("abierto");
    setCopiado(false);

    if (urlQr) {
      return;
    }
    try {
      // Import dinamico: el codificador QR no entra en el bundle inicial.
      const { default: QRCode } = await import("qrcode");
      setUrlQr(
        await QRCode.toDataURL(window.location.href, {
          width: 320,
          margin: 1,
          // Nivel M: tolera un 15 % de dano y sigue siendo legible desde una
          // pantalla, que es como se va a escanear casi siempre.
          errorCorrectionLevel: "M",
        }),
      );
    } catch {
      // Sin QR el panel sigue sirviendo para copiar y compartir.
      setUrlQr(null);
    }
  }

  async function compartirNativo() {
    try {
      await navigator.share({ title: titulo, url: window.location.href });
    } catch {
      // El usuario cancelo la hoja del sistema; no es un error que mostrar.
    }
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopiado(true);
    } catch {
      // Safari en http, o permiso denegado: se selecciona el texto para que se
      // pueda copiar a mano en vez de dejar al usuario sin salida.
      document.querySelector<HTMLInputElement>("[data-testid=url-compartir]")?.select();
    }
  }

  return (
    <div className="flex flex-col gap-3" data-testid="compartir">
      <button
        type="button"
        onClick={() => (estado === "cerrado" ? abrir() : setEstado("cerrado"))}
        data-testid="abrir-compartir"
        aria-expanded={estado === "abierto"}
        className="press min-h-touch w-fit rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
      >
        {t("compartir")}
      </button>

      {estado === "abierto" && (
        <div
          data-testid="panel-compartir"
          className="flex flex-col gap-4 rounded-card border border-border-base bg-surface p-4"
        >
          <label className="flex flex-col gap-1">
            <span className="text-fluid-sm text-text-muted">{t("enlace")}</span>
            <input
              type="text"
              readOnly
              value={url}
              data-testid="url-compartir"
              onFocus={(e) => e.currentTarget.select()}
              className="min-h-touch rounded-card border border-border-base bg-surface-muted px-3 text-fluid-sm text-text"
            />
          </label>

          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={copiar}
              data-testid="copiar-enlace"
              className="press min-h-touch rounded-card bg-primary px-4 text-fluid-sm font-medium text-primary-fg"
            >
              {copiado ? t("copiado") : t("copiar")}
            </button>

            {hayShareNativo && (
              <button
                type="button"
                onClick={compartirNativo}
                data-testid="compartir-nativo"
                className="press min-h-touch rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text"
              >
                {t("compartirNativo")}
              </button>
            )}
          </div>

          {urlQr && (
            <div className="flex flex-col items-center gap-2">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={urlQr}
                alt={t("codigoQrDe", { titulo })}
                data-testid="codigo-qr"
                className="size-40 rounded-card bg-sobre-foto-solido p-2"
              />
              <span className="text-fluid-sm text-text-muted">{t("escaneaParaAbrir")}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
