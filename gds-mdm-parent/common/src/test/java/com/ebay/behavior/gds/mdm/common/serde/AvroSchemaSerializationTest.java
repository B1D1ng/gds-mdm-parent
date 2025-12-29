package com.ebay.behavior.gds.mdm.common.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AvroSchemaSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @Setter
    static class TestModel {
        private long id;
        private String name;

        @JsonSerialize(using = AvroSchemaSerializer.class)
        private Schema avroSchema;
    }

    @Test
    void serialize() throws JsonProcessingException {
        String schemaJson = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"username\",\"type\":\"string\"},{\"name\":\"email\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);

        val model = new TestModel();
        model.setId(1);
        model.setName("Test Model");
        model.setAvroSchema(schema);

        val json = objectMapper.writeValueAsString(model);

        assertThat(json).contains("\"avroSchema\":\"" + schemaJson.replace("\"", "\\\"") + "\"");
    }
}
