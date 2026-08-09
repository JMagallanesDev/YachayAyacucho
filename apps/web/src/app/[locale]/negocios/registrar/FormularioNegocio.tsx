"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useLocale, useTranslations } from "next-intl";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { env } from "@/lib/env";
import { categoriasDeNegocio, registrarNegocio } from "@/lib/negocios";
import type { CategoriaNegocio, NuevoNegocio } from "@/types/negocio";

interface Distrito {
  id: string;
  nombre: string;
  provincia: string;
}

/**
 * Alta de un negocio en el directorio (RF-104).
 *
 * <p>El formulario <strong>no tiene ningun control de estado</strong>, y no es
 * un olvido: el negocio nace PENDIENTE y solo un administrador lo mueve. Se dice
 * antes de enviar, no despues, para que nadie espere verse publicado al
 * instante.</p>
 */
export function FormularioNegocio() {
  const t = useTranslations("negocios");
  const idioma = useLocale();
  const router = useRouter();
  const { comprobando } = useSesionRequerida();

  const [categorias, setCategorias] = useState<CategoriaNegocio[]>([]);
  const [distritos, setDistritos] = useState<Distrito[]>([]);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enviado, setEnviado] = useState(false);

  const [datos, setDatos] = useState<NuevoNegocio>({
    nombre: "",
    categoriaId: "",
    distritoId: "",
    telefono: "",
    whatsapp: "",
    direccion: "",
    horarioTexto: "",
    traducciones: [{ idioma: "ES", descripcion: "" }],
  });

  useEffect(() => {
    categoriasDeNegocio(idioma).then(setCategorias).catch(() => setCategorias([]));

    fetch(`${env.apiUrl}/distritos`)
      .then((r) => (r.ok ? r.json() : []))
      .then(setDistritos)
      .catch(() => setDistritos([]));
  }, [idioma]);

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setError(null);

    try {
      await registrarNegocio(datos, idioma);
      setEnviado(true);
    } catch (fallo) {
      setError(fallo instanceof Error ? fallo.message : t("errorGenerico"));
    } finally {
      setEnviando(false);
    }
  }

  if (comprobando) {
    return null;
  }

  if (enviado) {
    return (
      <div
        role="status"
        data-testid="negocio-registrado"
        className="flex flex-col gap-3 rounded-card border border-border-base bg-surface p-6"
      >
        <strong className="text-fluid-xl text-text">{t("gracias")}</strong>
        <p className="text-fluid-base text-text-muted">{t("quedaPendiente")}</p>
        <button
          type="button"
          onClick={() => router.push(`/${idioma}/perfil/mi-negocio`)}
          className="press min-h-touch w-fit rounded-card bg-primary px-5 py-2 text-fluid-sm font-medium text-primary-fg"
        >
          {t("verMiNegocio")}
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-5" data-testid="formulario-negocio">
      {/* El precio se dice ANTES de rellenar nada, no en la pantalla final. */}
      <p className="rounded-card bg-surface-muted p-4 text-fluid-sm text-text-muted">
        {t("avisoAprobacion")}
      </p>

      <label className="flex flex-col gap-1">
        <span className="text-fluid-sm text-text-muted">{t("nombreDelNegocio")}</span>
        <input
          type="text"
          required
          maxLength={200}
          value={datos.nombre}
          onChange={(e) => setDatos({ ...datos, nombre: e.target.value })}
          data-testid="negocio-nombre"
          className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
        />
      </label>

      <div className="flex flex-wrap gap-3">
        <label className="flex flex-1 flex-col gap-1">
          <span className="text-fluid-sm text-text-muted">{t("categoria")}</span>
          <select
            required
            value={datos.categoriaId}
            onChange={(e) => setDatos({ ...datos, categoriaId: e.target.value })}
            data-testid="negocio-categoria"
            className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
          >
            <option value="">{t("elegir")}</option>
            {categorias.map((categoria) => (
              <option key={categoria.id} value={categoria.id}>
                {categoria.nombre}
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-1 flex-col gap-1">
          <span className="text-fluid-sm text-text-muted">{t("distrito")}</span>
          <select
            required
            value={datos.distritoId}
            onChange={(e) => setDatos({ ...datos, distritoId: e.target.value })}
            data-testid="negocio-distrito"
            className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
          >
            <option value="">{t("elegir")}</option>
            {distritos.map((distrito) => (
              <option key={distrito.id} value={distrito.id}>
                {distrito.nombre} ({distrito.provincia})
              </option>
            ))}
          </select>
        </label>
      </div>

      <label className="flex flex-col gap-1">
        <span className="text-fluid-sm text-text-muted">{t("descripcion")}</span>
        <textarea
          rows={3}
          maxLength={2000}
          value={datos.traducciones?.[0]?.descripcion ?? ""}
          onChange={(e) =>
            setDatos({ ...datos, traducciones: [{ idioma: "ES", descripcion: e.target.value }] })
          }
          data-testid="negocio-descripcion"
          className="rounded-card border border-border-base bg-surface p-3 text-text"
        />
      </label>

      <div className="flex flex-wrap gap-3">
        <label className="flex flex-1 flex-col gap-1">
          <span className="text-fluid-sm text-text-muted">{t("telefono")}</span>
          <input
            type="tel"
            maxLength={30}
            value={datos.telefono}
            onChange={(e) => setDatos({ ...datos, telefono: e.target.value })}
            data-testid="negocio-telefono"
            className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
          />
        </label>

        <label className="flex flex-1 flex-col gap-1">
          <span className="text-fluid-sm text-text-muted">{t("whatsapp")}</span>
          <input
            type="tel"
            maxLength={30}
            placeholder="966 123 456"
            value={datos.whatsapp}
            onChange={(e) => setDatos({ ...datos, whatsapp: e.target.value })}
            data-testid="negocio-whatsapp"
            className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
          />
        </label>
      </div>

      <label className="flex flex-col gap-1">
        <span className="text-fluid-sm text-text-muted">{t("direccion")}</span>
        <input
          type="text"
          maxLength={255}
          value={datos.direccion}
          onChange={(e) => setDatos({ ...datos, direccion: e.target.value })}
          data-testid="negocio-direccion"
          className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
        />
      </label>

      <label className="flex flex-col gap-1">
        <span className="text-fluid-sm text-text-muted">{t("horario")}</span>
        <input
          type="text"
          maxLength={255}
          placeholder={t("horarioEjemplo")}
          value={datos.horarioTexto}
          onChange={(e) => setDatos({ ...datos, horarioTexto: e.target.value })}
          data-testid="negocio-horario"
          className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text"
        />
      </label>

      {error && (
        <p role="alert" data-testid="error-negocio" className="rounded-card bg-danger-subtle p-4 text-text">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={enviando}
        data-testid="enviar-negocio"
        className="press min-h-touch w-fit rounded-card bg-primary px-6 py-3 text-fluid-base font-medium text-primary-fg disabled:opacity-50"
      >
        {enviando ? t("enviando") : t("solicitarAlta")}
      </button>
    </form>
  );
}
