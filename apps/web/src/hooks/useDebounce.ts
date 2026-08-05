"use client";

import { useEffect, useState } from "react";

/**
 * Retrasa un valor hasta que deja de cambiar durante `retardo` ms.
 *
 * <p>Se usa en el buscador (RF-02): sin esto, cada tecla lanzaria una
 * peticion, de modo que escribir "catedral" dispararia ocho consultas de las
 * que solo importa la ultima. Con 300 ms, se busca cuando el usuario hace una
 * pausa natural al escribir.</p>
 */
export function useDebounce<T>(valor: T, retardo = 300): T {
  const [conRetardo, setConRetardo] = useState(valor);

  useEffect(() => {
    const temporizador = setTimeout(() => setConRetardo(valor), retardo);
    // Cada pulsacion cancela el temporizador anterior: por eso solo sobrevive
    // la ultima.
    return () => clearTimeout(temporizador);
  }, [valor, retardo]);

  return conRetardo;
}
