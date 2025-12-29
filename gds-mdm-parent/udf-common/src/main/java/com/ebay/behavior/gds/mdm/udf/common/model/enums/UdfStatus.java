package com.ebay.behavior.gds.mdm.udf.common.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;

public enum UdfStatus {
    CREATED("CREATED"),
    READY_TO_DEPLOY("READY_TO_DEPLOY"),
    DEPLOYED_TO_TEST("DEPLOYED_TO_TEST"),
    DEPLOYED_TO_TEST_FAILED("DEPLOYED_TO_TEST_FAILED"),
    APPROVED("APPROVED"),
    PROMOTED_TO_PROD("PROMOTED_TO_PROD"),
    PROMOTED_TO_PROD_FAILED("PROMOTED_TO_PROD_FAILED"),
    CANCELLED("CANCELLED");

    private final String value;

    UdfStatus(String value) {
        this.value = value;
    }

    @JsonCreator
    public static UdfStatus fromValue(String text) {
        for (val b : UdfStatus.values()) {
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
