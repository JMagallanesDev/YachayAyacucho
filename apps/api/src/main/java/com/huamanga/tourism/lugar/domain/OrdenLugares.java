package com.huamanga.tourism.lugar.domain;

/**
 * Criterios de ordenacion del listado (RF-06).
 *
 * <p>El codigo numerico es lo que viaja a la consulta nativa: SQL no entiende
 * de enums de Java, y pasar el nombre obligaria a construir la clausula
 * ORDER BY concatenando texto, que es justo por donde entra una inyeccion
 * SQL. Con un entero, el valor es inofensivo aunque llegue manipulado.</p>
 */
public enum OrdenLugares {

    /** Por nombre. El orden por defecto de un catalogo. */
    ALFABETICO(0),

    /** Mejor valorados: media de las resenas publicadas (RF-06). */
    MEJOR_VALORADOS(1),

    /** Mas visitados: check-ins acumulados (RF-06). */
    MAS_VISITADOS(2);

    private final int codigo;

    OrdenLugares(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }
}
