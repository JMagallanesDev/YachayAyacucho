package com.huamanga.tourism.auth.exception;

/**
 * El refresh token no sirve: desconocido, caducado, revocado o ya usado.
 *
 * <p>El motivo concreto se registra en el log del servidor, pero la respuesta
 * al cliente es siempre la misma: quien intenta renovar con un token que no
 * es suyo no necesita saber por que ha fallado.</p>
 */
public class RefreshTokenInvalidoException extends RuntimeException {

    public RefreshTokenInvalidoException(String motivoInterno) {
        super(motivoInterno);
    }
}
