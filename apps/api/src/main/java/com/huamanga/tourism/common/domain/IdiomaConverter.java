package com.huamanga.tourism.common.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persiste {@link Idioma} como su codigo ISO ({@code es}, {@code en}) y no
 * como el nombre del enum.
 *
 * <p>{@code autoApply = true} lo aplica a las 8 tablas de traduccion sin que
 * ninguna tenga que declararlo. Asi el valor en la BD coincide con el CHECK
 * de las migraciones y con los locales de next-intl del frontend.</p>
 */
@Converter(autoApply = true)
public class IdiomaConverter implements AttributeConverter<Idioma, String> {

    @Override
    public String convertToDatabaseColumn(Idioma idioma) {
        return idioma == null ? null : idioma.getCodigo();
    }

    @Override
    public Idioma convertToEntityAttribute(String codigo) {
        return codigo == null ? null : Idioma.desdeCodigo(codigo);
    }
}
