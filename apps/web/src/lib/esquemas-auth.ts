import { z } from "zod";

/**
 * Esquemas Zod de los formularios de autenticacion.
 *
 * Son la fuente de verdad, como manda el CLAUDE.md: de aqui salen tanto la
 * validacion en el navegador como los tipos de TypeScript, asi que no pueden
 * desincronizarse entre si.
 *
 * Ojo: esto valida, no protege. La validacion real vive en el backend, con
 * Bean Validation y los CHECK de PostgreSQL. Esta capa solo evita un viaje al
 * servidor para decirle al usuario algo que ya se sabe aqui.
 */

export const esquemaLogin = z.object({
  email: z.string().min(1, "Escribe tu correo").email("Ese correo no parece valido"),
  password: z.string().min(1, "Escribe tu contrasena"),
});

export const esquemaRegistro = z.object({
  nombre: z
    .string()
    .min(1, "Escribe tu nombre")
    .max(120, "El nombre es demasiado largo"),
  email: z
    .string()
    .min(1, "Escribe tu correo")
    .email("Ese correo no parece valido")
    .max(255, "El correo es demasiado largo"),
  // Misma politica que el backend, palabra por palabra.
  password: z
    .string()
    .min(8, "Usa al menos 8 caracteres")
    .max(72, "La contrasena no puede pasar de 72 caracteres")
    .regex(/[a-z]/, "Incluye al menos una minuscula")
    .regex(/[A-Z]/, "Incluye al menos una mayuscula")
    .regex(/\d/, "Incluye al menos un numero"),
});

export type DatosLogin = z.infer<typeof esquemaLogin>;
export type DatosRegistro = z.infer<typeof esquemaRegistro>;
