"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

/**
 * Estado de servidor con TanStack Query.
 *
 * <p>El {@code QueryClient} se crea con {@code useState}, no como constante
 * de modulo. La diferencia importa: en el servidor un modulo se comparte
 * entre peticiones, asi que un cliente global mezclaria en la misma cache los
 * datos de visitantes distintos. Con useState, cada renderizado tiene el
 * suyo.</p>
 *
 * <p>Reparto de responsabilidades del CLAUDE.md: aqui vive el estado que
 * viene del servidor; el estado de cliente (sesion, ubicacion) vive en
 * Zustand, y no se mezclan.</p>
 */
export function ProveedorQuery({ children }: { children: React.ReactNode }) {
  const [clienteQuery] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // 5 minutos, como fija el plan. Durante ese tiempo TanStack Query
            // sirve de su cache sin volver a preguntar.
            staleTime: 5 * 60 * 1000,
            // El servidor ya entrego datos frescos en el HTML: refrescar nada
            // mas montar seria un viaje inutil y un parpadeo.
            refetchOnWindowFocus: false,
            retry: 1,
          },
        },
      }),
  );

  return <QueryClientProvider client={clienteQuery}>{children}</QueryClientProvider>;
}
