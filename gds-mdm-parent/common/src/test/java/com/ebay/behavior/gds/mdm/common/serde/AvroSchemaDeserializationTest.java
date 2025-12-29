package com.ebay.behavior.gds.mdm.common.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AvroSchemaDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @Setter
    static class TestModel {
        private long id;
        private String name;

        @JsonDeserialize(using = AvroSchemaDeserializer.class)
        private Schema avroSchema;
    }

    private String generateJson(String schemaJson) {
        return """
                {
                    "id": 1,
                    "name": "Test Model",
                    "avroSchema": "%s"
                }
                """.formatted(schemaJson.replace("\"", "\\\""));
    }

    @Test
    void deserialize() throws JsonProcessingException {
        String schemaJson = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"username\",\"type\":\"string\"},{\"name\":\"email\",\"type\":\"string\"}]}";
        val json = generateJson(schemaJson);

        val model = objectMapper.readValue(json, TestModel.class);

        assertThat(model.getAvroSchema().toString()).isEqualTo(schemaJson);
    }
}
