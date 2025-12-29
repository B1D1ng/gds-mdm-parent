package com.ebay.behavior.gds.mdm.signal.common.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.apache.avro.Schema;

@Converter
public class AvroSchemaConverter implements AttributeConverter<Schema, String> {

    @Override
    public String convertToDatabaseColumn(Schema schema) {
        return schema.toString(); // Convert Schema to JSON string
    }

    @Override
    public Schema convertToEntityAttribute(String schemaString) {
        return new Schema.Parser().parse(schemaString); // Parse JSON string to Schema
    }
}