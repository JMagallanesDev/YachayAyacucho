"use client";

import { useLocale, useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { LugarCard } from "@/components/lugares/LugarCard";
import { useSesionRequerida } from "@/components/useSesionRequerida";
import { Link } from "@/i18n/navegacion";
import { misFavoritos } from "@/lib/participacion";
import type { LugarResumen } from "@/types/lugar";

/**
 * Lugares guardados (RF-35).
 *
 * <p>Reutiliza {@link LugarCard}, la misma tarjeta del catalogo: asi los
 * favoritos traen su insignia de abierto/cerrado y su distancia a pie sin
 * duplicar una linea de interfaz.</p>
 */
export function ListaFavoritos() {
  const t = useTranslations("participacion");
  const idioma = useLocale();
  const { comprobando } = useSesionRequerida();

  const [lugares, setLugares] = useState<LugarResumen[] | null>(null);

  const cargar = useCallback(() => misFavoritos(idioma), [idioma]);

  useEffect(() => {
    if (!comprobando) {
      cargar()
        .then((pagina) => setLugares(pagina.content))
        .catch(() => setLugares([]));
    }
  }, [comprobando, cargar]);

  if (comprobando || lugares === null) {
    return null;
  }

  if (lugares.length === 0) {
    return (
      <p data-testid="sin-favoritos" className="rounded-card bg-surface-muted p-6 text-text-muted">
        {t.rich("sinFavoritos", {
          enlace: (texto) => (
            <Link href="/lugares" className="font-medium text-primary underline-offset-4 hover:underline">
              {texto}
            </Link>
          ),
        })}
      </p>
    );
  }

  return (
    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" data-testid="lista-favoritos">
      {lugares.map((lugar) => (
        <li key={lugar.id}>
          <LugarCard lugar={lugar} />
        </li>
      ))}
    </ul>
  );
}
