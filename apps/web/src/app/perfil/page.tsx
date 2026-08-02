import { PanelSesion } from "./PanelSesion";

export const metadata = {
  title: "Mi perfil",
};

/**
 * Ruta privada. El `proxy.ts` impide llegar aqui sin cookie de sesion, pero
 * los datos se piden al backend con el access token, que es quien autoriza de
 * verdad.
 */
export default function PaginaPerfil() {
  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-8 px-5 py-12">
      <h1 className="text-fluid-2xl font-bold text-text">Mi perfil</h1>
      <PanelSesion />
    </main>
  );
}
