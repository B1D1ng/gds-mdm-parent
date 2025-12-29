package com.ebay.behavior.gds.mdm.common.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.val;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;

public class ObjectListToStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        val token = parser.currentToken();
        if (token == JsonToken.START_ARRAY) {
            List<Object> list = parser.readValueAs(new TypeReference<List<Object>>() {
            });
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(COMMA));
        }
        if (token == JsonToken.VALUE_STRING) {
            return parser.getText();
        }

        return parser.getValueAsString();
    }
}