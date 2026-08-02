package com.huamanga.tourism.auth.exception;

/**
 * Credenciales incorrectas en el login.
 *
 * <p>Un unico tipo de excepcion para "el correo no existe" y "la contrasena
 * no coincide". No es pereza: distinguirlos en la respuesta permitiria a
 * cualquiera averiguar que correos estan registrados en el sistema
 * probandolos uno a uno.</p>
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Credenciales invalidas");
    }
}
