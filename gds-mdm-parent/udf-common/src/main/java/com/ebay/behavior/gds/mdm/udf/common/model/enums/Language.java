package com.ebay.behavior.gds.mdm.udf.common.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Language {
    JAVA("JAVA"),
    PYTHON("PYTHON");

    private final String value;

    Language(String value) {
        this.value = value;
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
