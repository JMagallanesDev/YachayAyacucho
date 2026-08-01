package com.huamanga.tourism.common.domain;

/**
 * Idiomas del contenido de dominio.
 *
 * <p>Se guarda el codigo ({@code es}, {@code en}) y no el nombre del enum,
 * para que coincida con los locales de next-intl en el frontend y con el
 * CHECK de las tablas de traduccion.</p>
 *
 * <p>El sistema detecta tambien frances y aleman, pero no los traduce: caen
 * al ingles por fallback (RF-64). Por eso no aparecen aqui.</p>
 */
public enum Idioma {

    ES("es"),
    EN("en");

    private final String codigo;

    Idioma(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static Idioma desdeCodigo(String codigo) {
        for (Idioma idioma : values()) {
            if (idioma.codigo.equalsIgnoreCase(codigo)) {
                return idioma;
            }
        }
        throw new IllegalArgumentException("Idioma no soportado: " + codigo);
    }
}
