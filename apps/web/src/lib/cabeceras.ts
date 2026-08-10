/**
 * Cabeceras de las llamadas publicas al API.
 *
 * <p><strong>Por que existe.</strong> Al generar las paginas estaticas, el
 * servidor de Next pide al API los lugares, los eventos y los negocios en los
 * dos idiomas: mas de cien llamadas en unos segundos desde una sola direccion,
 * que es exactamente el patron que el rate limiting existe para frenar. El
 * resultado era que {@code next build} abortaba con un 429 a mitad del
 * prerenderizado.</p>
 *
 * <p>La solucion es que el servidor se identifique con un secreto compartido.
 * Y la parte importante es <strong>cuando NO se envia</strong>:</p>
 *
 * <ul>
 *   <li>La variable <strong>no lleva prefijo {@code NEXT_PUBLIC_}</strong>, asi
 *       que Next no la inyecta en el bundle del navegador: en el cliente vale
 *       {@code undefined} y la cabecera no se anade.</li>
 *   <li>Ademas se comprueba explicitamente que estamos en el servidor. Es
 *       redundante a proposito: si alguien renombrara la variable con el
 *       prefijo publico por descuido, esta segunda barrera impediria que el
 *       secreto acabara viajando en las peticiones del navegador, donde
 *       cualquiera podria leerlo del inspector de red.</li>
 * </ul>
 *
 * <p>Un visitante nunca queda exento del limite; solo el servidor.</p>
 */
export function cabecerasPublicas(extra?: Record<string, string>): Record<string, string> {
  const cabeceras: Record<string, string> = { Accept: "application/json", ...extra };

  const enElServidor = typeof window === "undefined";
  const secreto = process.env.YACHAY_TOKEN_INTERNO;

  if (enElServidor && secreto) {
    cabeceras["X-Yachay-Interno"] = secreto;
  }

  return cabeceras;
}
