package com.ebay.behavior.gds.mdm.common.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectListToStringDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @Setter
    static class TestModel {
        private long planId;
        private String name;
        private String domain;

        @JsonDeserialize(using = ObjectListToStringDeserializer.class)
        private String owners;
    }

    private String generateJson(Object owners) throws JsonProcessingException {
        return """
                {
                    "planId": 1,
                    "owners": %s,
                    "name": "Test Signal",
                    "domain": "Test Domain"
                }
                """.formatted(
                owners instanceof String str ? "\"" + str + "\"" : objectMapper.writeValueAsString(owners)
        );
    }

    @Test
    void deserialize() throws JsonProcessingException {
        val json = generateJson(List.of("owner1", "owner2", "owner3"));
        val model = objectMapper.readValue(json, TestModel.class);

        assertThat(model.getOwners()).isEqualTo("owner1,owner2,owner3");
    }

    @Test
    void deserialize_emptyList() throws JsonProcessingException {
        val json = generateJson(List.of());
        val model = objectMapper.readValue(json, TestModel.class);

        assertThat(model.getOwners()).isEqualTo("");
    }

    @Test
    void deserialize_int() throws JsonProcessingException {
        val json = generateJson(1);
        val model = objectMapper.readValue(json, TestModel.class);

        assertThat(model.getOwners()).isEqualTo("1");
    }

    @Test
    void deserialize_intList() throws JsonProcessingException {
        val json = generateJson(List.of(1));
        val model = objectMapper.readValue(json, TestModel.class);

        assertThat(model.getOwners()).isEqualTo("1");
    }
}