"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { ErrorApi, iniciarSesion, registrar } from "@/lib/auth";
import { esquemaRegistro, type DatosRegistro } from "@/lib/esquemas-auth";
import { useSesion } from "@/stores/sesion";

export function FormularioRegistro() {
  const router = useRouter();
  const guardarSesion = useSesion((estado) => estado.iniciar);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosRegistro>({ resolver: zodResolver(esquemaRegistro) });

  async function enviar(datos: DatosRegistro) {
    setErrorGeneral(null);
    try {
      await registrar(datos);
      // Registro y login son endpoints separados en el backend; encadenarlos
      // aqui evita pedirle al recien registrado que escriba lo mismo dos veces.
      const sesion = await iniciarSesion({ email: datos.email, password: datos.password });
      guardarSesion(sesion.accessToken, sesion.usuario);
      router.push("/perfil");
      router.refresh();
    } catch (error) {
      setErrorGeneral(
        error instanceof ErrorApi
          ? (error.problema.detail ?? "No se pudo crear la cuenta")
          : "No se pudo conectar con el servidor",
      );
    }
  }

  return (
    <form onSubmit={handleSubmit(enviar)} className="flex flex-col gap-5" noValidate>
      <Campo
        id="nombre"
        etiqueta="Nombre"
        autoComplete="name"
        error={errors.nombre?.message}
        {...register("nombre")}
      />
      <Campo
        id="email"
        etiqueta="Correo"
        tipo="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register("email")}
      />
      <Campo
        id="password"
        etiqueta="Contrasena"
        tipo="password"
        autoComplete="new-password"
        ayuda="Minimo 8 caracteres, con mayuscula, minuscula y numero"
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
        {isSubmitting ? "Creando cuenta..." : "Crear cuenta"}
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
