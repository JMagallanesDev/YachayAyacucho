"use client";

import { useTranslations } from "next-intl";
import { useCallback, useEffect, useState } from "react";

import { useSesionRequerida } from "@/components/useSesionRequerida";
import { cambiarUsuario, usuariosDelPanel } from "@/lib/admin";
import { ErrorApi } from "@/lib/auth";
import type { EstadoUsuario, NombreRol, UsuarioAdmin } from "@/types/admin";

const ROLES: NombreRol[] = ["VISITANTE", "USUARIO", "NEGOCIO", "ADMIN"];

/**
 * Gestion de cuentas y roles (RF-51).
 *
 * <p><strong>Aqui no aparece ninguna contrasena, y no por omision.</strong> El
 * DTO del backend no tiene ese campo, asi que no hay nada que ocultar en la
 * interfaz: el dato no llega. Tampoco hay forma de fijar una clave desde el
 * panel — si alguien pierde la suya, el camino es la recuperacion por correo y
 * no que otra persona le elija una que despues conoce.</p>
 *
 * <p>El selector de rol de la propia cuenta viene <strong>desactivado</strong>.
 * Es cortesia, no seguridad: quien lo fuerce recibira un 422 del servidor, que
 * es quien de verdad decide.</p>
 */
export function GestionUsuarios() {
  const t = useTranslations("panel");
  const { comprobando } = useSesionRequerida();

  const [usuarios, setUsuarios] = useState<UsuarioAdmin[]>([]);
  const [sinPermisos, setSinPermisos] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [trabajando, setTrabajando] = useState<string | null>(null);

  const cargar = useCallback(() => usuariosDelPanel(), []);

  const alFallar = useCallback((fallo: unknown) => {
    if (fallo instanceof ErrorApi && fallo.estado === 403) {
      setSinPermisos(true);
      return;
    }
    // El backend manda el motivo traducido en el ProblemDetail: se muestra tal
    // cual en vez de inventar aqui un mensaje que diria menos.
    setError(fallo instanceof Error ? fallo.message : t("errorCarga"));
  }, [t]);

  useEffect(() => {
    if (!comprobando) {
      cargar().then(setUsuarios).catch(alFallar);
    }
  }, [comprobando, cargar, alFallar]);

  async function cambiar(usuario: UsuarioAdmin, cambios: { rol?: NombreRol; estado?: EstadoUsuario }) {
    setError(null);
    setTrabajando(usuario.id);
    try {
      await cambiarUsuario(usuario.id, cambios);
      setUsuarios(await cargar());
    } catch (fallo) {
      alFallar(fallo);
    } finally {
      setTrabajando(null);
    }
  }

  if (comprobando || sinPermisos) {
    return null;
  }

  return (
    <section className="flex flex-col gap-4" data-testid="gestion-usuarios">
      <h2 className="text-fluid-xl font-semibold text-text">
        {t("usuarios", { total: usuarios.length })}
      </h2>

      {error && (
        <p role="alert" data-testid="error-usuario" className="rounded-card bg-danger-subtle p-4 text-text">
          {error}
        </p>
      )}

      <ul className="flex flex-col gap-3" data-testid="lista-usuarios">
        {usuarios.map((usuario) => (
          <li
            key={usuario.id}
            data-testid="usuario-gestionable"
            data-email={usuario.email}
            data-rol={usuario.rol}
            data-estado={usuario.estado}
            data-tuya={usuario.esTuCuenta}
            className="flex flex-wrap items-end justify-between gap-3 rounded-card border border-border-base bg-surface p-4"
          >
            <div className="flex min-w-48 flex-col gap-0.5">
              <span className="text-fluid-base font-medium text-text">{usuario.nombre}</span>
              <span className="text-fluid-sm text-text-muted">{usuario.email}</span>
              {usuario.esTuCuenta && (
                <span data-testid="marca-tu-cuenta" className="text-fluid-sm text-accent">
                  {t("tuCuenta")}
                </span>
              )}
            </div>

            <div className="flex flex-wrap items-end gap-3">
              <label className="flex flex-col gap-1">
                <span className="text-fluid-sm text-text-muted">{t("rol")}</span>
                <select
                  value={usuario.rol}
                  disabled={usuario.esTuCuenta || trabajando === usuario.id}
                  onChange={(e) => cambiar(usuario, { rol: e.target.value as NombreRol })}
                  data-testid="selector-rol"
                  className="min-h-touch rounded-card border border-border-base bg-surface px-3 text-text disabled:opacity-50"
                >
                  {ROLES.map((rol) => (
                    <option key={rol} value={rol}>
                      {t(`rolNombre.${rol}`)}
                    </option>
                  ))}
                </select>
              </label>

              <button
                type="button"
                disabled={trabajando === usuario.id}
                onClick={() =>
                  cambiar(usuario, {
                    estado: usuario.estado === "SUSPENDIDO" ? "ACTIVO" : "SUSPENDIDO",
                  })
                }
                data-testid="alternar-estado"
                className="press min-h-touch rounded-card border border-border-strong px-3 text-fluid-sm font-medium text-text disabled:opacity-60"
              >
                {usuario.estado === "SUSPENDIDO" ? t("reactivar") : t("suspender")}
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
