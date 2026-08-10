import type { ReactNode } from "react";

/**
 * Entrada escalonada de una lista.
 *
 * <p><strong>Esto es CSS y no Motion, y el motivo importa.</strong> La primera
 * version lo hacia con {@code motion.li} y tenia dos defectos que solo se ven
 * cuando se prueban de verdad:</p>
 *
 * <ol>
 *   <li><strong>El contenido quedaba invisible sin JavaScript.</strong> Motion
 *       escribe {@code opacity: 0} en linea al renderizar en el servidor y lo
 *       levanta al hidratar. Si el JavaScript tarda o falla, el listado de
 *       lugares —contenido publico que debe leerse siempre— no aparece.</li>
 *   <li><strong>Discordancia de hidratacion con «menos movimiento».</strong> El
 *       servidor no puede saber la preferencia del visitante, asi que
 *       renderizaba el estado animado y el navegador el estado final. React
 *       tiraba el arbol entero.</li>
 * </ol>
 *
 * <p>Una animacion CSS no tiene ninguno de los dos problemas: corre en el primer
 * pintado sin esperar a nada, y el bloque {@code prefers-reduced-motion} de
 * {@code globals.css} ya la neutraliza globalmente. Motion se reserva para lo
 * que CSS no puede hacer —las transiciones de elemento compartido de la barra de
 * navegacion y del interruptor de tema, con {@code layoutId}—, que es donde
 * aporta algo que no se puede escribir a mano.</p>
 *
 * <p>El escalonado viaja como variable CSS y no como estilo calculado: asi el
 * retardo lo aplica la hoja de estilos y el marcado queda limpio.</p>
 */

/** Tope del escalonado: a partir del elemento 12 todos entran a la vez. */
const MAXIMO_ESCALONADO = 12;

export function Aparecer({
  children,
  indice = 0,
  className = "",
  comoLista = false,
  ...resto
}: {
  children: ReactNode;
  /** Posicion en la lista, para el escalonado. */
  indice?: number;
  className?: string;
  /** Renderiza un <li> en vez de un <div>, para no romper listas. */
  comoLista?: boolean;
} & Record<string, unknown>) {
  const Elemento = comoLista ? "li" : "div";

  return (
    <Elemento
      className={`aparecer ${className}`}
      style={{ "--indice": Math.min(indice, MAXIMO_ESCALONADO) } as React.CSSProperties}
      {...resto}
    >
      {children}
    </Elemento>
  );
}
