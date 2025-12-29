package com.ebay.behavior.gds.mdm.udf.common.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;

public enum UdfType {

    UDF("UDF"),
    UDAF("UDAF"),
    UDTF("UDTF");

    private final String value;

    UdfType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static UdfType fromValue(String text) {
        for (val b : UdfType.values()) {
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
