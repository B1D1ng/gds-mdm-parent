package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;

public enum UdcDataSourceType {
    STAGED("cjs"),
    QA("gds"),
    TEST("gds_test");

    private final String value;

    UdcDataSourceType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static UdcDataSourceType fromValue(String text) {
        for (val b : UdcDataSourceType.values()) {
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
