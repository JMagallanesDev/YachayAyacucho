import { FormularioRegistro } from "./FormularioRegistro";

export const metadata = {
  title: "Crear cuenta",
};

export default function PaginaRegistro() {
  return (
    <main className="mx-auto flex min-h-svh w-full max-w-md flex-col justify-center gap-8 px-5 py-12">
      <header className="flex flex-col gap-2">
        <h1 className="text-fluid-2xl font-bold text-text">Crear cuenta</h1>
        <p className="text-fluid-base text-text-muted">
          Explorar el patrimonio no exige cuenta. La necesitas para guardar favoritos, opinar y
          coleccionar sellos.
        </p>
      </header>

      <FormularioRegistro />
    </main>
  );
}
