package com.ebay.behavior.gds.mdm.common.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaTypeTest {

    @Test
    void convert() {
        var javaType = JavaType.INTEGER;

        var converted = javaType.convert("100");

        assertThat(converted).isEqualTo(100);
    }

    @Test
    void toSchema_array() {
        var javaType = JavaType.LIST;

        var schema = javaType.toSchema().toString();

        assertThat(schema).isEqualTo("{\"type\":\"array\",\"items\":\"string\"}");
    }

    @Test
    void toSchema_map() {
        var javaType = JavaType.MAP;

        var schema = javaType.toSchema().toString();

        assertThat(schema).isEqualTo("{\"type\":\"map\",\"values\":\"string\"}");
    }

    @Test
    void toSchema_long() {
        var javaType = JavaType.LONG;

        var schema = javaType.toSchema().toString();

        assertThat(schema).isEqualTo("\"long\"");
    }

    @Test
    void toSchema_string() {
        var javaType = JavaType.STRING;

        var schema = javaType.toSchema().toString();

        assertThat(schema).isEqualTo("\"string\"");
    }
}