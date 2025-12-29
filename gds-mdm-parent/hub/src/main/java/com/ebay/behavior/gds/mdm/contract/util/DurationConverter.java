package com.ebay.behavior.gds.mdm.contract.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Converter
public class DurationConverter implements AttributeConverter<Duration, String> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(Duration attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize attribute to JSON %s".formatted(attribute), e);
        }
    }

    @Override
    public Duration convertToEntityAttribute(String dbData) {
        if (StringUtils.isEmpty(dbData)) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, Duration.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not deserialize JSON to Object %s".formatted(dbData), e);
        }
    }
}
