package com.ebay.behavior.gds.mdm.common.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.val;
import org.apache.avro.Schema;

import java.io.IOException;

public class AvroSchemaDeserializer extends JsonDeserializer<Schema> {

    @Override
    public Schema deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        val schemaJson = parser.getText();
        return new Schema.Parser().parse(schemaJson);
    }
}
