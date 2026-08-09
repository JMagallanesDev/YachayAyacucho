import { pedirAutenticado } from "@/lib/auth";
import { env } from "@/lib/env";
import type {
  Actividad,
  Dashboard,
  EstadoUsuario,
  NombreRol,
  TipoPagina,
  UsuarioAdmin,
} from "@/types/admin";

/** Cliente del panel de administracion (RF-51, RF-52, RF-56). */

export function metricas(idioma: string) {
  return pedirAutenticado<Dashboard>(`/admin/dashboard?idioma=${idioma.toUpperCase()}`);
}

export function usuariosDelPanel() {
  return pedirAutenticado<UsuarioAdmin[]>("/admin/usuarios");
}

export function cambiarUsuario(
  usuarioId: string,
  cambios: { rol?: NombreRol; estado?: EstadoUsuario },
) {
  return pedirAutenticado<UsuarioAdmin>(`/admin/usuarios/${usuarioId}`, {
    method: "PATCH",
    body: JSON.stringify(cambios),
  });
}

export function registroDeActividad(limite = 50) {
  return pedirAutenticado<Actividad[]>(`/admin/actividad?limite=${limite}`);
}

/**
 * Anota una visita a una seccion (RF-52b).
 *
 * <p>No lleva token ni ningun identificador: el servidor cuenta por una huella
 * efimera que no puede recuperar quien la genero. Y no se espera la respuesta —
 * si falla, falla en silencio: una metrica no puede estropear una pagina.</p>
 */
export function anotarVisita(tipo: TipoPagina): void {
  try {
    const url = `${env.apiUrl}/analitica/visitas?tipo=${tipo}`;

    // sendBeacon sobrevive a que el visitante cierre la pestana justo despues,
    // que es precisamente cuando una visita corta se perderia.
    if (typeof navigator !== "undefined" && navigator.sendBeacon) {
      navigator.sendBeacon(url);
      return;
    }
    void fetch(url, { method: "POST", keepalive: true }).catch(() => {});
  } catch {
    /* una metrica nunca rompe la navegacion */
  }
}
