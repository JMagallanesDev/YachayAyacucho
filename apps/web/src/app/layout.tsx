import type { ReactNode } from "react";

import "./globals.css";

/**
 * Layout raiz minimo.
 *
 * <p>Con enrutado por idioma, quien pone {@code <html>} y {@code <body>} es el
 * layout de {@code [locale]}, porque el atributo {@code lang} depende del
 * idioma activo y aqui todavia no se conoce. Next exige un layout raiz de
 * todos modos, asi que este se limita a dejar pasar a sus hijos.</p>
 */
export default function LayoutRaiz({ children }: { children: ReactNode }) {
  return children;
}
