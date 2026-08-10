"use client";

import { useTranslations } from "next-intl";

import { useValorDelCliente } from "@/components/useValorDelCliente";
import { diasHasta } from "@/lib/fechas";
import type { FechaISO } from "@/lib/fechas";

/**
 * Cuanto falta para un evento (RF-84).
 *
 * <p><strong>Por que recibe `diasIniciales` como prop.</strong> Es la leccion
 * del Bloque 8: si el primer render del navegador calculara los dias por su
 * cuenta podria no coincidir con el HTML que mando el servidor, y React tiraria
 * el arbol entero con un aviso de hidratacion. Aqui el servidor ya los calculo
 * —en la zona de Ayacucho— y ese numero es la instantanea del servidor; el
 * navegador recalcula despues de hidratar, que es lo que corrige el caso real:
 * el HTML venia de una cache (la del router, o la del CDN en produccion) y
 * entretanto ha cambiado el dia.</p>
 *
 * <p>No cuenta horas ni minutos a proposito. Una agenda cultural se planifica en
 * dias, un contador al segundo obligaria a un temporizador permanente, y en una
 * fiesta que dura una semana esa precision no significa nada.</p>
 */
export function CuentaRegresiva({
  fechaInicio,
  fechaFin,
  diasIniciales,
}: {
  fechaInicio: FechaISO;
  fechaFin: FechaISO;
  diasIniciales: number;
}) {
  const t = useTranslations("agenda");

  // Dos valores primitivos y no un objeto: `useSyncExternalStore` compara la
  // instantanea con Object.is, y un objeto nuevo en cada llamada seria un bucle.
  const dias = useValorDelCliente(
    () => diasHasta(fechaInicio),
    () => diasIniciales,
  );
  const diasParaElFinal = useValorDelCliente(
    () => diasHasta(fechaFin),
    () => diasIniciales,
  );

  const enCurso = dias <= 0 && diasParaElFinal >= 0;

  return (
    <span
      data-testid="cuenta-regresiva"
      data-dias={dias}
      className="w-fit rounded-full bg-accent-subtle px-3 py-1 text-fluid-sm font-medium text-accent-text"
    >
      {enCurso ? t("ocurreAhora") : dias === 1 ? t("faltaUnDia") : t("faltanDias", { dias })}
    </span>
  );
}
