"use client";

import { useEffect, useState } from "react";

import { tokensDeLienzo } from "@/lib/tokens";

type Tokens = ReturnType<typeof tokensDeLienzo>;

/**
 * Los tokens de color que necesitan el mapa y los graficos, **al dia con el
 * tema** (RF-89, RF-94).
 *
 * <p>Leerlos una sola vez al montar no basta: si alguien cambia a modo oscuro
 * con el mapa abierto, las chinchetas y los ejes se quedarian con la paleta
 * clara sobre fondo negro. Este hook vuelve a leerlos cuando cambia el tema, y
 * escucha las <strong>dos</strong> formas en que eso puede pasar:</p>
 *
 * <ol>
 *   <li>El interruptor escribe {@code data-theme} en {@code <html>}: lo detecta
 *       un {@code MutationObserver} sobre ese atributo.</li>
 *   <li>Con el tema en "sistema" no hay atributo que observar —manda la media
 *       query—, asi que ademas se escucha {@code prefers-color-scheme}. Es el
 *       caso por defecto y el que se olvida.</li>
 * </ol>
 */
export function useTokensDeLienzo(): Tokens | null {
  // Arranca en null y no con valores por defecto: en el servidor no hay
  // documento, y devolver colores inventados haria que el primer fotograma se
  // pintara con una paleta que no es la del usuario.
  const [tokens, setTokens] = useState<Tokens | null>(null);

  useEffect(() => {
    const releer = () => setTokens(tokensDeLienzo());
    releer();

    const observador = new MutationObserver(releer);
    observador.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["data-theme"],
    });

    const consulta = window.matchMedia("(prefers-color-scheme: dark)");
    consulta.addEventListener("change", releer);

    return () => {
      observador.disconnect();
      consulta.removeEventListener("change", releer);
    };
  }, []);

  return tokens;
}
