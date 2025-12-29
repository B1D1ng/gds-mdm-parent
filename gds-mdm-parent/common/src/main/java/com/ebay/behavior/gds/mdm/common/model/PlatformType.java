package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;

public enum PlatformType {
    CJS("CJS"),
    ESP("ESP"),
    EJS("EJS"),
    ITEM("ITEM"),
    TRANSACTION("TRANSACTION");

    private final String value;

    PlatformType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PlatformType fromValue(String text) {
        for (val b : PlatformType.values()) {
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
