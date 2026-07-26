package com.built4u.pos.common.tenant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts between Java boolean and Oracle's "Y"/"N" string convention used by
 * the older-style tables.
 */
@Converter
public class YesNoConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean value) {
        if (value == null) return null;
        return value ? "Y" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String value) {
        if (value == null) return null;
        return "Y".equalsIgnoreCase(value);
    }
}
