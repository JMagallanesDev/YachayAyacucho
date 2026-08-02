import { z } from "zod";

/**
 * Esquemas Zod de los formularios de autenticacion.
 *
 * <p>Son la fuente de verdad de las **reglas**, como manda el CLAUDE.md: de
 * aqui salen tanto la validacion en el navegador como los tipos de
 * TypeScript, asi que no pueden desincronizarse entre si.</p>
 *
 * <p>Los **textos**, en cambio, vienen de next-intl. Por eso los esquemas se
 * construyen con una funcion que recibe el traductor en lugar de tener los
 * mensajes incrustados: un mensaje en espanol dentro de un formulario en
 * ingles rompe la traduccion justo donde mas se nota, que es cuando el
 * usuario se equivoca.</p>
 *
 * <p>Esto valida, no protege. La validacion real vive en el backend, con Bean
 * Validation y los CHECK de PostgreSQL.</p>
 */

type Traductor = (clave: string) => string;

export function esquemaLogin(t: Traductor) {
  return z.object({
    email: z
      .string()
      .min(1, t("correoObligatorio"))
      .email(t("correoInvalido")),
    password: z.string().min(1, t("contrasenaObligatoria")),
  });
}

export function esquemaRegistro(t: Traductor) {
  return z.object({
    nombre: z.string().min(1, t("nombreObligatorio")).max(120, t("nombreLargo")),
    email: z
      .string()
      .min(1, t("correoObligatorio"))
      .email(t("correoInvalido"))
      .max(255, t("correoLargo")),
    // Misma politica que el backend, palabra por palabra.
    password: z
      .string()
      .min(8, t("contrasenaCorta"))
      .max(72, t("contrasenaLarga"))
      .regex(/[a-z]/, t("contrasenaMinuscula"))
      .regex(/[A-Z]/, t("contrasenaMayuscula"))
      .regex(/\d/, t("contrasenaNumero")),
  });
}

export type DatosLogin = z.infer<ReturnType<typeof esquemaLogin>>;
export type DatosRegistro = z.infer<ReturnType<typeof esquemaRegistro>>;
