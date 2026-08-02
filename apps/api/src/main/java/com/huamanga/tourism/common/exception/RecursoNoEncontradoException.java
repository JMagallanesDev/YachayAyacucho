package com.huamanga.tourism.common.exception;

/**
 * El recurso pedido no existe, o no existe para quien pregunta.
 *
 * <p>Se traduce en un 404. Tambien se usa cuando el recurso existe pero no
 * deberia ser visible —un lugar en borrador para un visitante anonimo—,
 * porque un 403 confirmaria que el recurso existe, y un borrador es
 * precisamente lo que aun no debe saberse que existe.</p>
 */
public class RecursoNoEncontradoException extends RuntimeException {

    private final String tipo;
    private final String identificador;

    public RecursoNoEncontradoException(String tipo, String identificador) {
        super("No existe %s con identificador %s".formatted(tipo, identificador));
        this.tipo = tipo;
        this.identificador = identificador;
    }

    public String getTipo() {
        return tipo;
    }

    public String getIdentificador() {
        return identificador;
    }
}
