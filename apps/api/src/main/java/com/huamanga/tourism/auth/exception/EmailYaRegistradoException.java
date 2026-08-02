package com.huamanga.tourism.auth.exception;

/** El correo ya tiene una cuenta asociada. Se traduce a HTTP 409. */
public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException() {
        super("Ya existe una cuenta con ese correo");
    }
}
