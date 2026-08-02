package com.huamanga.tourism.lugar.evento;

/**
 * Un lugar se creo, se edito o se dio de baja.
 *
 * <p>Lo publica {@code LugarService} y lo consume el listener de
 * revalidacion. Se usa un evento y no una llamada directa para que el service
 * no sepa nada de webhooks ni de Next.js: su trabajo es guardar el lugar, y
 * quien quiera enterarse se suscribe.</p>
 *
 * @param slug identifica la pagina que hay que regenerar
 */
public record LugarGuardadoEvent(String slug) {
}
