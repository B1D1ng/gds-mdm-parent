package com.ebay.behavior.gds.mdm.dec.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null) {
            return null;
        }

        if (attribute.isEmpty()) {
            return "";
        }

        return String.join(COMMA, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (StringUtils.isEmpty(dbData)) {
            return Collections.emptyList();
        }

        return Arrays.asList(StringUtils.split(dbData, COMMA));
    }
}
