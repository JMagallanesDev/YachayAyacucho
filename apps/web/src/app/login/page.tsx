import { FormularioLogin } from "./FormularioLogin";

export const metadata = {
  title: "Iniciar sesion",
};

/**
 * Server Component: solo compone la pagina. Toda la interactividad vive en el
 * formulario, que es la hoja del arbol, siguiendo la regla del CLAUDE.md de
 * mantener 'use client' en las hojas.
 */
export default function PaginaLogin() {
  return (
    <main className="mx-auto flex min-h-svh w-full max-w-md flex-col justify-center gap-8 px-5 py-12">
      <header className="flex flex-col gap-2">
        <h1 className="text-fluid-2xl font-bold text-text">Iniciar sesion</h1>
        <p className="text-fluid-base text-text-muted">
          Entra para guardar favoritos, escribir resenas y sellar tu pasaporte patrimonial.
        </p>
      </header>

      <FormularioLogin />
    </main>
  );
}
