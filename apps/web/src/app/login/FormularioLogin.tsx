"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { ErrorApi, iniciarSesion } from "@/lib/auth";
import { esquemaLogin, type DatosLogin } from "@/lib/esquemas-auth";
import { useSesion } from "@/stores/sesion";

export function FormularioLogin() {
  const router = useRouter();
  const guardarSesion = useSesion((estado) => estado.iniciar);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosLogin>({ resolver: zodResolver(esquemaLogin) });

  async function enviar(datos: DatosLogin) {
    setErrorGeneral(null);
    try {
      const sesion = await iniciarSesion(datos);
      // El access token se queda en memoria; el refresh ya viaja en la cookie.
      guardarSesion(sesion.accessToken, sesion.usuario);
      router.push("/perfil");
      router.refresh();
    } catch (error) {
      // El backend responde lo mismo tanto si el correo no existe como si la
      // contrasena es incorrecta, para no revelar que cuentas existen.
      setErrorGeneral(
        error instanceof ErrorApi
          ? (error.problema.detail ?? "No se pudo iniciar sesion")
          : "No se pudo conectar con el servidor",
      );
    }
  }

  return (
    <form onSubmit={handleSubmit(enviar)} className="flex flex-col gap-5" noValidate>
      <div className="flex flex-col gap-2">
        <label htmlFor="email" className="text-fluid-sm font-medium text-text">
          Correo
        </label>
        <input
          id="email"
          type="email"
          autoComplete="email"
          {...register("email")}
          aria-invalid={Boolean(errors.email)}
          className="min-h-touch rounded-card border border-border-base bg-surface px-4 text-text"
        />
        {errors.email && (
          <p role="alert" className="text-fluid-sm text-danger">
            {errors.email.message}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-2">
        <label htmlFor="password" className="text-fluid-sm font-medium text-text">
          Contrasena
        </label>
        <input
          id="password"
          type="password"
          autoComplete="current-password"
          {...register("password")}
          aria-invalid={Boolean(errors.password)}
          className="min-h-touch rounded-card border border-border-base bg-surface px-4 text-text"
        />
        {errors.password && (
          <p role="alert" className="text-fluid-sm text-danger">
            {errors.password.message}
          </p>
        )}
      </div>

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
        {isSubmitting ? "Entrando..." : "Entrar"}
      </button>
    </form>
  );
}
