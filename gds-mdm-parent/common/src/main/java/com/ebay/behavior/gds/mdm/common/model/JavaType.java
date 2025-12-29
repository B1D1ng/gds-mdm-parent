package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.avro.Schema;
import org.apache.commons.beanutils.ConvertUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@SuppressWarnings("PMD.FieldNamingConventions")
public enum JavaType {

    STRING("java.lang.String", String.class),
    LONG("java.lang.Long", Long.class),
    INTEGER("java.lang.Integer", Integer.class),
    BIG_INTEGER("java.math.BigInteger", BigInteger.class),
    BOOLEAN("java.lang.Boolean", Boolean.class),
    DOUBLE("java.lang.Double", Double.class),
    FLOAT("java.lang.Float", Float.class),
    BYTE("java.lang.Byte", Byte.class),
    OBJECT("java.lang.Object", Object.class),
    MAP("java.util.Map<String, String>", Map.class),
    LIST("java.util.List", List.class);

    private final String value;
    private final Class<?> type;

    JavaType(String value, Class<?> type) {
        this.value = value;
        this.type = type;
    }

    @JsonCreator
    public static JavaType fromValue(String text) {
        for (JavaType b : JavaType.values()) {
            if (String.valueOf(b.value).equalsIgnoreCase(String.valueOf(text))) {
                return b;
            }
        }
        throw new IllegalArgumentException(String.format("Unexpected value: %s", text));
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public Class<?> getType() {
        return type;
    }

    public Object convert(Object value) {
        return ConvertUtils.convert(value, type);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public Schema toSchema() {
        return switch (this) {
            case STRING, OBJECT, BIG_INTEGER -> Schema.create(Schema.Type.STRING);
            case LONG -> Schema.create(Schema.Type.LONG);
            case INTEGER -> Schema.create(Schema.Type.INT);
            case BOOLEAN -> Schema.create(Schema.Type.BOOLEAN);
            case DOUBLE -> Schema.create(Schema.Type.DOUBLE);
            case FLOAT -> Schema.create(Schema.Type.FLOAT);
            case BYTE -> Schema.create(Schema.Type.BYTES);
            case LIST -> Schema.createArray(Schema.create(Schema.Type.STRING));
            case MAP -> Schema.createMap(Schema.create(Schema.Type.STRING));
        };
    }
}
