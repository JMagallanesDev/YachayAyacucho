"use client";

import { useEffect, useState } from "react";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { ErrorApi, pedirAutenticado } from "@/lib/auth";

type Resumen = { usuarios: number; lugares: number };

export function ResumenAdmin() {
  const { comprobando } = useSesionRequerida();
  const [resumen, setResumen] = useState<Resumen | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (comprobando) {
      return;
    }
    pedirAutenticado<Resumen>("/admin/resumen")
      .then(setResumen)
      .catch((fallo) => {
        // 403 es la respuesta correcta para un usuario autenticado sin rol
        // ADMIN: la autorizacion la decide el backend, no esta pantalla.
        setError(
          fallo instanceof ErrorApi && fallo.estado === 403
            ? "Tu cuenta no tiene permisos de administracion."
            : "No se pudo cargar el resumen.",
        );
      });
  }, [comprobando]);

  if (error) {
    return (
      <p role="alert" className="rounded-card bg-danger-subtle p-4 text-text">
        {error}
      </p>
    );
  }

  if (!resumen) {
    return <p className="text-text-muted">Cargando resumen...</p>;
  }

  return (
    <dl className="grid grid-cols-2 gap-4">
      <div className="rounded-card border border-border-base bg-surface p-6">
        <dt className="text-fluid-sm text-text-muted">Usuarios</dt>
        <dd className="text-fluid-2xl font-bold text-text">{resumen.usuarios}</dd>
      </div>
      <div className="rounded-card border border-border-base bg-surface p-6">
        <dt className="text-fluid-sm text-text-muted">Lugares</dt>
        <dd className="text-fluid-2xl font-bold text-text">{resumen.lugares}</dd>
      </div>
    </dl>
  );
}
