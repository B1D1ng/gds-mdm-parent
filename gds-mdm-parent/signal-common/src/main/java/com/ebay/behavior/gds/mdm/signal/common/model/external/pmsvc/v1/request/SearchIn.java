package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchIn {
    ID("id"),
    NAME("name"),
    DESCRIPTION("description"),
    LIFECYCLE_STATE("lifecycle state"),
    OWNER("owner"),
    SOJOURNER_NAME("sojourner name");

    private final String value;

    SearchIn(String value) {
        this.value = value;
    }

    @JsonCreator
    public static SearchIn fromValue(String text) {
        for (SearchIn b : SearchIn.values()) {
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
