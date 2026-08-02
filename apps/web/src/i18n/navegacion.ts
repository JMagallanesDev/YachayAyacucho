import { createNavigation } from "next-intl/navigation";

import { routing } from "./routing";

/**
 * APIs de navegacion conscientes del idioma.
 *
 * <p>Hay que usar estas y no las de `next/navigation` a secas siempre que la
 * navegacion cambie o dependa del idioma. Dos razones:</p>
 * <ul>
 *   <li>Anaden el prefijo de idioma solas, asi que
 *       {@code router.push("/perfil")} lleva a {@code /es/perfil} sin
 *       concatenar nada a mano.</li>
 *   <li><strong>Escriben la cookie NEXT_LOCALE al cambiar de idioma.</strong>
 *       Sustituir el prefijo de la URL manualmente cambia la pagina pero no
 *       deja constancia de la eleccion, y la siguiente visita a "/" vuelve al
 *       idioma detectado del navegador. Es exactamente el fallo que tuvo la
 *       primera version del selector.</li>
 * </ul>
 */
export const { Link, redirect, usePathname, useRouter, getPathname } = createNavigation(routing);
