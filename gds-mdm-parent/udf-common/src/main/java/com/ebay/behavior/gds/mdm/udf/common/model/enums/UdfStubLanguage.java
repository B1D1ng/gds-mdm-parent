package com.ebay.behavior.gds.mdm.udf.common.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;

public enum UdfStubLanguage {
    JAVA("JAVA"),
    FLINK_SQL("FLINK_SQL"),
    SPARK_SQL("SPARK_SQL"),
    IRIS("IRIS")
    ;

    private final String value;

    UdfStubLanguage(String value) {
        this.value = value;
    }

    @JsonCreator
    public static UdfStubLanguage fromValue(String text) {
        for (val b : UdfStubLanguage.values()) {
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
