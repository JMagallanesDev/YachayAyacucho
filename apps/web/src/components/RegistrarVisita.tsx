"use client";

import { useEffect, useRef } from "react";

import { anotarVisita } from "@/lib/admin";
import type { TipoPagina } from "@/types/admin";

/**
 * Anota una visita a la seccion en la que se monta (RF-52b).
 *
 * <p><strong>Por que lo cuenta el navegador y no el servidor.</strong> Casi
 * todas las paginas del sitio se sirven con ISR: el servidor las renderiza una
 * vez y despues reparte la misma copia durante minutos. Contar ahi mediria
 * regeneraciones de cache, no personas.</p>
 *
 * <p>No envia ningun identificador —ni token, ni cookie, ni sesion—: solo el
 * nombre de la seccion. Quien cuenta es el servidor, con una huella efimera de
 * la que no puede recuperar el origen, y que ademas ignora la peticion si esa
 * misma huella ya paso por aqui en la ultima media hora.</p>
 *
 * <p>El componente no pinta nada y jamas bloquea la pagina: si la llamada
 * falla, falla en silencio.</p>
 */
export function RegistrarVisita({ tipo }: { tipo: TipoPagina }) {
  // React 19 en modo estricto monta los efectos dos veces en desarrollo. Sin
  // esta guarda, cada visita se enviaria por duplicado; el throttling del
  // servidor lo absorberia, pero es mejor no mandar la segunda.
  const yaEnviada = useRef(false);

  useEffect(() => {
    if (yaEnviada.current) {
      return;
    }
    yaEnviada.current = true;
    anotarVisita(tipo);
  }, [tipo]);

  return null;
}
