import { ResumenAdmin } from "./ResumenAdmin";

export const metadata = {
  title: "Administracion",
};

/**
 * Panel de administracion. El dashboard completo llega en el Bloque 10.
 *
 * El `proxy.ts` impide entrar sin cookie de sesion, pero eso solo evita
 * mostrar el cascaron: quien llegue aqui sin rol ADMIN vera el titulo y un
 * mensaje de acceso denegado, porque el backend responde 403 a la llamada.
 */
export default function PaginaAdmin() {
  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-8 px-5 py-12">
      <h1 className="text-fluid-2xl font-bold text-text">Administracion</h1>
      <ResumenAdmin />
    </main>
  );
}
