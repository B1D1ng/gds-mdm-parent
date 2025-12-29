package com.ebay.behavior.gds.mdm.udf.common.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;

public enum UdfStubType {
    PUBLIC("PUBLIC"),
    PRIVATE("PRIVATE")
    ;

    private final String value;

    UdfStubType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static UdfStubType fromValue(String text) {
        for (val b : UdfStubType.values()) {
            if (String.valueOf(b.value).equals(String.valueOf(text))) {
                return b;
            }
        }
        throw new IllegalArgumentException(String.format("Unexpected value: %s", text));
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

