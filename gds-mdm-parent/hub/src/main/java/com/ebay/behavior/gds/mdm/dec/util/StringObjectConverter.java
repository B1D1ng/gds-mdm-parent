package com.ebay.behavior.gds.mdm.dec.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.apache.commons.lang3.StringUtils;

@Converter
public class StringObjectConverter implements AttributeConverter<Object, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize attribute to JSON", e);
        }
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        if (StringUtils.isEmpty(dbData)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not deserialize JSON to Object", e);
        }
    }
}
