/**
 * Esqueletos de carga (RF-96).
 *
 * <p><strong>La unica regla que importa: el esqueleto ocupa exactamente la caja
 * del contenido que sustituye.</strong> Un esqueleto que mide distinto no
 * reduce el desplazamiento de diseño, lo <em>causa</em>: el contenido llega, no
 * cabe, y todo lo de abajo salta. Por eso cada esqueleto de este archivo se
 * escribe junto al componente real y copia sus alturas.</p>
 *
 * <p>Server Component: no necesita JavaScript. El brillo es una animacion CSS,
 * que el bloque {@code prefers-reduced-motion} de globals.css ya detiene para
 * quien lo pide.</p>
 */

/** Una barra gris con el brillo de carga. */
export function Barra({ className = "" }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`block animate-pulse rounded-card bg-esqueleto ${className}`}
    />
  );
}

/**
 * Envoltorio accesible de una zona en carga.
 *
 * <p>{@code aria-busy} y el texto oculto son lo que hace que un lector de
 * pantalla anuncie «cargando» en vez de leer una fila de cajas vacias.</p>
 */
export function ZonaEnCarga({
  children,
  etiqueta,
  testid,
}: {
  children: React.ReactNode;
  etiqueta: string;
  testid?: string;
}) {
  return (
    <div role="status" aria-busy="true" data-testid={testid ?? "esqueleto"}>
      <span className="sr-only">{etiqueta}</span>
      {children}
    </div>
  );
}

/** Tarjeta de listado: dos lineas de texto y una etiqueta. */
export function EsqueletoTarjeta() {
  return (
    <div className="flex flex-col gap-2 rounded-card border border-border-base bg-surface p-4">
      <Barra className="h-6 w-24" />
      <Barra className="h-5 w-3/4" />
      <Barra className="h-4 w-full" />
      <Barra className="h-4 w-1/2" />
    </div>
  );
}

/** Rejilla de tarjetas, para los listados paginados. */
export function EsqueletoListado({ filas = 4, etiqueta }: { filas?: number; etiqueta: string }) {
  return (
    <ZonaEnCarga etiqueta={etiqueta} testid="esqueleto-listado">
      <ul className="grid gap-3 sm:grid-cols-2">
        {Array.from({ length: filas }, (_, i) => (
          <li key={i}>
            <EsqueletoTarjeta />
          </li>
        ))}
      </ul>
    </ZonaEnCarga>
  );
}

/** Bloque de metricas: tres cifras en fila. */
export function EsqueletoMetricas({ etiqueta }: { etiqueta: string }) {
  return (
    <ZonaEnCarga etiqueta={etiqueta} testid="esqueleto-metricas">
      <div className="grid grid-cols-3 gap-3">
        {Array.from({ length: 3 }, (_, i) => (
          <div key={i} className="flex flex-col gap-2 rounded-card bg-surface-muted p-3">
            <Barra className="h-4 w-16" />
            <Barra className="h-7 w-12" />
          </div>
        ))}
      </div>
    </ZonaEnCarga>
  );
}
