import { obtenerSalud } from "@/lib/api";
import { env } from "@/lib/env";
import type { ComponenteSalud } from "@/types/health";

/**
 * Portada provisional del Bloque 0.
 *
 * Es un Server Component: el estado se consulta en el servidor y llega
 * al navegador como HTML ya renderizado, sin JavaScript de cliente.
 *
 * No es un "hello world" decorativo: comprueba de verdad que el
 * frontend habla con el backend y que el backend habla con PostgreSQL
 * y Redis. Si esta pagina se ve en verde, los cimientos estan puestos.
 *
 * Nota: los textos estan escritos directamente aqui porque next-intl
 * entra en el Bloque 3. A partir de ahi todo pasa por t('clave')
 * (RF-66) y esta pagina cede su sitio a la portada real.
 */

const NOMBRES_COMPONENTE: Record<string, string> = {
  postgresql: "PostgreSQL 16 + PostGIS",
  redis: "Redis",
};

const PALETA = [
  { nombre: "Retablo", clase: "bg-retablo-600", origen: "Rojo carmin de cochinilla" },
  { nombre: "Anil", clase: "bg-anil-800", origen: "Indigo de los textiles" },
  { nombre: "Quinua", clase: "bg-quinua-500", origen: "Ocre del barro cocido" },
  { nombre: "Sillar", clase: "bg-sillar-200", origen: "Piedra de los templos" },
  { nombre: "Puna", clase: "bg-puna-600", origen: "Verde de altura" },
];

export default async function PaginaInicio() {
  const resultado = await obtenerSalud();

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-2xl flex-col gap-8 px-5 py-12">
      <header className="flex flex-col gap-3">
        <span className="text-fluid-sm font-medium uppercase tracking-widest text-accent">
          Bloque 0 &middot; Cimientos
        </span>
        <h1 className="text-fluid-3xl font-bold text-text">{env.appName}</h1>
        <p className="text-fluid-base text-text-muted">
          Patrimonio cultural de Huamanga, Ayacucho. Esta pagina comprueba en vivo que el
          frontend, el API y la infraestructura estan conectados.
        </p>
      </header>

      <section
        aria-labelledby="titulo-estado"
        className="rounded-card border border-border-base bg-surface p-6 shadow-card"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 id="titulo-estado" className="text-fluid-lg font-semibold text-text">
            Estado del sistema
          </h2>
          <EtiquetaEstado
            ok={resultado.alcanzable && resultado.salud.status === "UP"}
            texto={
              !resultado.alcanzable
                ? "Sin conexion"
                : resultado.salud.status === "UP"
                  ? "Operativo"
                  : "Degradado"
            }
          />
        </div>

        {resultado.alcanzable ? (
          <ul className="mt-5 flex flex-col divide-y divide-border-base">
            <FilaComponente
              nombre="API Spring Boot"
              detalle={`Aplicacion ${resultado.salud.application}`}
              ok
              milisegundos={null}
            />
            {resultado.salud.components.map((componente: ComponenteSalud) => (
              <FilaComponente
                key={componente.name}
                nombre={NOMBRES_COMPONENTE[componente.name] ?? componente.name}
                detalle={componente.detail}
                ok={componente.status === "UP"}
                milisegundos={componente.responseTimeMs}
              />
            ))}
          </ul>
        ) : (
          <div className="mt-5 rounded-card bg-danger-subtle p-4">
            <p className="font-medium text-text">{resultado.motivo}</p>
            <p className="mt-1 text-fluid-sm text-text-muted">
              Comprueba que Docker esta levantado (<code>docker compose up -d</code>) y que el
              backend corre en <code>{env.apiUrl}</code>.
            </p>
          </div>
        )}
      </section>

      <section aria-labelledby="titulo-paleta" className="flex flex-col gap-4">
        <h2 id="titulo-paleta" className="text-fluid-lg font-semibold text-text">
          Paleta Ayacucho
        </h2>
        {/* Unico lugar donde se usan colores primitivos directamente:
            este bloque existe justamente para mostrarlos. El resto de la
            aplicacion consume solo tokens semanticos. */}
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-5">
          {PALETA.map((color) => (
            <li key={color.nombre} className="flex flex-col gap-2">
              <span
                className={`h-14 w-full rounded-card border border-border-base ${color.clase}`}
                aria-hidden="true"
              />
              <span className="text-fluid-sm font-medium text-text">{color.nombre}</span>
              <span className="text-fluid-sm text-text-muted">{color.origen}</span>
            </li>
          ))}
        </ul>
      </section>

      <footer className="mt-auto border-t border-border-base pt-6 text-fluid-sm text-text-muted">
        Tesis: &laquo;Aplicacion web para la publicacion de informacion del patrimonio cultural
        de Ayacucho, 2026&raquo;.
      </footer>
    </main>
  );
}

function EtiquetaEstado({ ok, texto }: { ok: boolean; texto: string }) {
  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-fluid-sm font-medium ${
        ok ? "bg-success-subtle text-success" : "bg-danger-subtle text-danger"
      }`}
    >
      <span className={`size-2 rounded-full ${ok ? "bg-success" : "bg-danger"}`} aria-hidden="true" />
      {texto}
    </span>
  );
}

function FilaComponente({
  nombre,
  detalle,
  ok,
  milisegundos,
}: {
  nombre: string;
  detalle: string;
  ok: boolean;
  milisegundos: number | null;
}) {
  return (
    <li className="flex items-center justify-between gap-4 py-3">
      <span className="flex flex-col">
        <span className="font-medium text-text">{nombre}</span>
        <span className="text-fluid-sm text-text-muted">{detalle}</span>
      </span>
      <span className="flex shrink-0 items-center gap-3">
        {milisegundos !== null && (
          <span className="text-fluid-sm tabular-nums text-text-muted">{milisegundos} ms</span>
        )}
        <span
          className={`size-2.5 rounded-full ${ok ? "bg-success" : "bg-danger"}`}
          role="img"
          aria-label={ok ? "Operativo" : "Caido"}
        />
      </span>
    </li>
  );
}
