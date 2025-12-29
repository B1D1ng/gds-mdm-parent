package com.ebay.behavior.gds.mdm.dec.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class StringListConverterTest {

    @Test
    void convertToDatabaseColumn_EmptyList() {
        StringListConverter converter = new StringListConverter();

        // Test with null input
        assertThat(converter.convertToDatabaseColumn(null)).isNull();

        // Test with empty list
        assertThat(converter.convertToDatabaseColumn(Collections.emptyList())).isEqualTo("");
    }

    @Test
    void convertToEntityAttribute_EmptyString() {
        StringListConverter converter = new StringListConverter();

        // Test with null input
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();

        // Test with empty string
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
    }
}
