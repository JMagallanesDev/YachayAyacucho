"use client";

import { useSyncExternalStore } from "react";

/** No hay nada a lo que suscribirse: el valor solo se lee al hidratar. */
const sinSuscripcion = () => () => {};

/**
 * Un valor que el servidor no puede conocer, sin discordancia de hidratacion.
 *
 * <p><strong>El problema que resuelve.</strong> Hay datos que dependen del
 * navegador —que dia es hoy segun su reloj, por ejemplo— y que el servidor no
 * puede calcular igual, sobre todo cuando la pagina se sirvio desde la cache de
 * ISR hace horas. Calcularlos durante el render rompe la hidratacion; leerlos
 * en un {@code useEffect} y guardarlos con {@code setState} funciona, pero
 * encadena un render extra y ESLint lo desaconseja con razon.</p>
 *
 * <p>{@code useSyncExternalStore} esta hecho exactamente para esto: React usa
 * la instantanea del servidor durante el render inicial y la hidratacion —de
 * modo que ambos coinciden byte a byte— y cambia a la del cliente en cuanto
 * termina.</p>
 *
 * <p>Aviso: {@code enCliente} debe devolver un <strong>primitivo</strong> o un
 * valor estable entre llamadas. Si devuelve un objeto nuevo cada vez, React
 * entra en un bucle de renders.</p>
 */
export function useValorDelCliente<T>(enCliente: () => T, enServidor: () => T): T {
  return useSyncExternalStore(sinSuscripcion, enCliente, enServidor);
}
