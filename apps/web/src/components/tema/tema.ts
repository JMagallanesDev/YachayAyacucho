/**
 * Preferencia de tema del usuario (RF-94).
 *
 * <p>Modulo neutro, sin directiva: lo comparten el script en linea del layout
 * —que corre en el servidor como texto— y el interruptor de cliente. Es la
 * leccion del Bloque 11: lo que comparten servidor y cliente no vive en un
 * archivo con {@code "use client"}.</p>
 */

export type Tema = "claro" | "oscuro" | "sistema";

export const TEMAS: Tema[] = ["claro", "oscuro", "sistema"];

/** Clave de localStorage. Con prefijo para no chocar con nada del dominio. */
export const CLAVE_TEMA = "yachay:tema";

/**
 * Script que corre ANTES del primer pintado.
 *
 * <p><strong>Por que existe.</strong> El tema elegido vive en
 * {@code localStorage}, que el servidor no puede leer. Sin esto, quien tiene el
 * modo oscuro puesto veria un fogonazo blanco en cada carga: el HTML llega con
 * el tema claro, React hidrata, y solo entonces se corrige. Ese destello es el
 * detalle que mas delata que una web no es una app.</p>
 *
 * <p>Va como {@code dangerouslySetInnerHTML} en el {@code <head>} y es
 * sincrono a proposito: bloquea el pintado unas decimas de milisegundo, que es
 * exactamente lo que hace falta para que no haya fogonazo.</p>
 *
 * <p>Escribe {@code data-theme} solo para las elecciones explicitas. Con
 * "sistema" no escribe nada, y entonces manda la media query
 * {@code prefers-color-scheme} de los tokens: el sistema operativo decide, y si
 * el usuario lo cambia mientras la pagina esta abierta, cambia sola sin
 * JavaScript.</p>
 */
export const GUION_ANTI_DESTELLO = `
(function () {
  try {
    var t = localStorage.getItem("${CLAVE_TEMA}");
    if (t === "claro" || t === "oscuro") {
      document.documentElement.dataset.theme = t === "oscuro" ? "dark" : "light";
    }
  } catch (e) {
    /* Modo privado de Safari puede lanzar al leer. Sin tema guardado,
       manda la preferencia del sistema, que es un buen valor por defecto. */
  }
})();
`;

/** Aplica un tema al documento y lo recuerda. */
export function aplicarTema(tema: Tema) {
  const raiz = document.documentElement;

  if (tema === "sistema") {
    // Quitar el atributo es lo que devuelve el mando a la media query.
    delete raiz.dataset.theme;
  } else {
    raiz.dataset.theme = tema === "oscuro" ? "dark" : "light";
  }

  try {
    localStorage.setItem(CLAVE_TEMA, tema);
  } catch {
    /* Sin persistencia el tema dura esta visita, que es mejor que fallar. */
  }
}

/** Lee la preferencia guardada. "sistema" cuando no hay ninguna. */
export function leerTema(): Tema {
  try {
    const guardado = localStorage.getItem(CLAVE_TEMA);
    return guardado === "claro" || guardado === "oscuro" ? guardado : "sistema";
  } catch {
    return "sistema";
  }
}
