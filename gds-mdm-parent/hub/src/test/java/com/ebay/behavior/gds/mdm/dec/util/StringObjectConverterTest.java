package com.ebay.behavior.gds.mdm.dec.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StringObjectConverterTest {

    @Test
    void convertToDatabaseColumn_NullObject() {
        StringObjectConverter converter = new StringObjectConverter();

        // Test with null input
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_EmptyString() {
        StringObjectConverter converter = new StringObjectConverter();

        // Test with null input
        assertThat(converter.convertToEntityAttribute(null)).isNull();

        // Test with empty string
        assertThat(converter.convertToEntityAttribute("")).isNull();
    }

    @Test
    void convertToDatabaseColumn_ValidObject() {
        StringObjectConverter converter = new StringObjectConverter();

        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        String json = converter.convertToDatabaseColumn(map);
        assertThat(json).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void convertToDatabaseColumn_ThrowsException() {
        StringObjectConverter converter = new StringObjectConverter();
        // Jackson cannot serialize objects with circular references
        class Node { Node ref; }
        Node node = new Node();
        node.ref = node;
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> converter.convertToDatabaseColumn(node));
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Could not serialize attribute to JSON");
    }

    @Test
    void convertToEntityAttribute_ValidJson() {
        StringObjectConverter converter = new StringObjectConverter();
        String json = "{\"foo\":123}";
        Object result = converter.convertToEntityAttribute(json);
        assertThat(result).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) result).get("foo")).isEqualTo(123);
    }

    @Test
    void convertToEntityAttribute_InvalidJson() {
        StringObjectConverter converter = new StringObjectConverter();
        String invalidJson = "not a json";
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> converter.convertToEntityAttribute(invalidJson));
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
