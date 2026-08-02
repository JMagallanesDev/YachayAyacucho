"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";

import { ErrorApi, iniciarSesion, registrar } from "@/lib/auth";
import { esquemaRegistro, type DatosRegistro } from "@/lib/esquemas-auth";
import { useSesion } from "@/stores/sesion";

export function FormularioRegistro() {
  const t = useTranslations("registro");
  const tv = useTranslations("validacion");
  const idioma = useLocale();
  const router = useRouter();
  const guardarSesion = useSesion((estado) => estado.iniciar);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const esquema = useMemo(() => esquemaRegistro(tv), [tv]);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosRegistro>({ resolver: zodResolver(esquema) });

  async function enviar(datos: DatosRegistro) {
    setErrorGeneral(null);
    try {
      await registrar(datos);
      // Registro y login son endpoints separados en el backend; encadenarlos
      // aqui evita pedirle al recien registrado que escriba lo mismo dos veces.
      const sesion = await iniciarSesion({ email: datos.email, password: datos.password });
      guardarSesion(sesion.accessToken, sesion.usuario);
      router.push(`/${idioma}/perfil`);
      router.refresh();
    } catch (error) {
      setErrorGeneral(
        error instanceof ErrorApi
          ? (error.problema.detail ?? t("errorGenerico"))
          : t("errorConexion"),
      );
    }
  }

  return (
    <form onSubmit={handleSubmit(enviar)} className="flex flex-col gap-5" noValidate>
      <Campo
        id="nombre"
        etiqueta={t("nombre")}
        autoComplete="name"
        error={errors.nombre?.message}
        {...register("nombre")}
      />
      <Campo
        id="email"
        etiqueta={t("correo")}
        tipo="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register("email")}
      />
      <Campo
        id="password"
        etiqueta={t("contrasena")}
        tipo="password"
        autoComplete="new-password"
        ayuda={t("ayudaContrasena")}
        error={errors.password?.message}
        {...register("password")}
      />

      {errorGeneral && (
        <p role="alert" className="rounded-card bg-danger-subtle p-3 text-fluid-sm text-text">
          {errorGeneral}
        </p>
      )}

      <button
        type="submit"
        disabled={isSubmitting}
        className="press min-h-touch rounded-card bg-primary px-5 font-medium text-primary-fg disabled:opacity-60"
      >
        {isSubmitting ? t("creando") : t("crear")}
      </button>
    </form>
  );
}

// ComponentPropsWithRef y no InputHTMLAttributes: en React 19 `ref` es una
// prop normal, y register() de React Hook Form la pasa entre las demas.
interface CampoProps extends React.ComponentPropsWithRef<"input"> {
  id: string;
  etiqueta: string;
  tipo?: string;
  ayuda?: string;
  error?: string;
}

function Campo({ id, etiqueta, tipo = "text", ayuda, error, ref, ...resto }: CampoProps) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-fluid-sm font-medium text-text">
        {etiqueta}
      </label>
      <input
        id={id}
        type={tipo}
        ref={ref}
        aria-invalid={Boolean(error)}
        aria-describedby={ayuda ? `${id}-ayuda` : undefined}
        className="min-h-touch rounded-card border border-border-base bg-surface px-4 text-text"
        {...resto}
      />
      {ayuda && (
        <p id={`${id}-ayuda`} className="text-fluid-sm text-text-muted">
          {ayuda}
        </p>
      )}
      {error && (
        <p role="alert" className="text-fluid-sm text-danger">
          {error}
        </p>
      )}
    </div>
  );
}
