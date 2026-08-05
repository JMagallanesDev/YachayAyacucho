/**
 * Esqueletos de carga (RF-96).
 *
 * <p>Ocupan <strong>exactamente</strong> el alto de una tarjeta real. Es la
 * diferencia entre un esqueleto util y uno danino: si midiera distinto, al
 * llegar los datos la pagina daria un salto y el CLS se dispararia por encima
 * del 0.1 que exige el RNF-24.</p>
 *
 * <p>Se prefieren a un spinner porque anticipan la forma de lo que viene: el
 * usuario entiende que va a recibir una lista de tarjetas antes de tenerla.</p>
 */
export function EsqueletoLugares({ cantidad = 6 }: { cantidad?: number }) {
  return (
    <ul
      className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
      aria-busy="true"
      data-testid="esqueleto-lugares"
    >
      {Array.from({ length: cantidad }, (_, i) => (
        <li
          key={i}
          className="flex h-52 flex-col gap-3 rounded-card border border-border-base bg-surface p-5"
        >
          <div className="flex gap-2">
            <span className="h-6 w-24 animate-pulse rounded-full bg-surface-muted" />
            <span className="h-6 w-20 animate-pulse rounded-full bg-surface-muted" />
          </div>
          <span className="h-6 w-3/4 animate-pulse rounded bg-surface-muted" />
          <span className="h-4 w-full animate-pulse rounded bg-surface-muted" />
          <span className="h-4 w-5/6 animate-pulse rounded bg-surface-muted" />
          <span className="mt-auto h-4 w-1/2 animate-pulse rounded bg-surface-muted" />
        </li>
      ))}
    </ul>
  );
}
