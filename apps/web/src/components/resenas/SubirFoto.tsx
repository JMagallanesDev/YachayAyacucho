"use client";

import { useTranslations } from "next-intl";
import { useEffect, useRef, useState } from "react";

import { ErrorApi } from "@/lib/auth";
import { misFotos, subirFoto } from "@/lib/resenas";
import { useSesion } from "@/stores/sesion";
import type { Foto } from "@/types/resena";

/** Debe coincidir con ValidadorImagen.TAMANO_MAXIMO_BYTES (RNF-15). */
const TAMANO_MAXIMO = 5 * 1024 * 1024;
const MAXIMO_FOTOS = 5;

/**
 * Subida de fotos de un lugar (RF-38).
 *
 * <p>La comprobacion de tamaño y tipo que hay aqui es <strong>solo comodidad</strong>:
 * evita gastar la subida de un archivo que va a ser rechazado. La validacion
 * que cuenta ocurre en el servidor, que mira los bytes; cualquier cosa
 * comprobada en el navegador se puede saltar con una peticion a mano.</p>
 *
 * <p>Las fotos propias se muestran con su estado, incluidas las pendientes: sin
 * eso, quien sube una foto y no la ve en la galeria piensa que se perdio y la
 * vuelve a subir.</p>
 */
export function SubirFoto({ slug }: { slug: string }) {
  const t = useTranslations("fotos");
  const usuario = useSesion((estado) => estado.usuario);
  const entrada = useRef<HTMLInputElement>(null);

  const [mias, setMias] = useState<Foto[]>([]);
  const [subiendo, setSubiendo] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  // Sin sesion no se pide nada y no hace falta limpiar el estado: el
  // componente entero no se pinta, asi que lo que quede en memoria no se ve.
  useEffect(() => {
    if (usuario) {
      void misFotos(slug).then(setMias);
    }
  }, [usuario, slug]);

  if (!usuario) {
    return null;
  }

  async function alElegir(evento: React.ChangeEvent<HTMLInputElement>) {
    const archivo = evento.target.files?.[0];
    if (!archivo) return;

    setError(null);
    setAviso(null);

    if (archivo.size > TAMANO_MAXIMO) {
      setError(t("demasiadoGrande"));
      evento.target.value = "";
      return;
    }

    setSubiendo(true);
    try {
      const subida = await subirFoto(slug, archivo);
      setMias((previas) => [subida, ...previas]);
      setAviso(t("enRevision"));
    } catch (fallo) {
      setError(
        fallo instanceof ErrorApi
          ? (fallo.problema?.detail ?? t("errorGenerico"))
          : t("errorGenerico"),
      );
    } finally {
      setSubiendo(false);
      // Se limpia para poder volver a elegir el MISMO archivo: sin esto, el
      // evento change no vuelve a dispararse y el segundo intento no hace nada.
      if (entrada.current) {
        entrada.current.value = "";
      }
    }
  }

  const alcanzoElLimite = mias.length >= MAXIMO_FOTOS;

  return (
    <section className="flex flex-col gap-3" data-testid="subir-foto">
      <h3 className="text-fluid-lg font-semibold text-text">{t("titulo")}</h3>
      <p className="text-fluid-sm text-text-muted">{t("explicacion")}</p>

      <label className="flex w-fit items-center">
        <input
          ref={entrada}
          type="file"
          // Sugerencia para el selector del sistema, no una garantia: el
          // servidor vuelve a comprobarlo mirando el contenido.
          accept="image/jpeg,image/png,image/webp"
          onChange={alElegir}
          disabled={subiendo || alcanzoElLimite}
          data-testid="entrada-foto"
          className="sr-only"
        />
        <span
          className={`press min-h-touch inline-flex items-center rounded-card border border-border-strong px-4 text-fluid-sm font-medium text-text ${
            subiendo || alcanzoElLimite ? "opacity-50" : "cursor-pointer"
          }`}
        >
          {subiendo ? t("subiendo") : alcanzoElLimite ? t("limite") : t("elegir")}
        </span>
      </label>

      {error && (
        <p role="alert" data-testid="error-foto" className="rounded-card bg-danger-subtle p-3 text-fluid-sm text-text">
          {error}
        </p>
      )}
      {aviso && (
        <p role="status" data-testid="aviso-foto" className="rounded-card bg-surface-muted p-3 text-fluid-sm text-text-muted">
          {aviso}
        </p>
      )}

      {mias.length > 0 && (
        <ul className="flex flex-wrap gap-3" data-testid="mis-fotos">
          {mias.map((foto) => (
            <li key={foto.id} className="flex flex-col items-center gap-1">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={foto.miniatura}
                alt=""
                width={96}
                height={96}
                className="size-24 rounded-card object-cover"
              />
              <span
                data-testid={`estado-foto-${foto.estado}`}
                className="text-fluid-sm text-text-muted"
              >
                {t(`estado.${foto.estado}`)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
