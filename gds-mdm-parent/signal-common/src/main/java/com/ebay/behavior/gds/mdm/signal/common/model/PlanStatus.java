package com.ebay.behavior.gds.mdm.signal.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;

public enum PlanStatus {

    CREATED("CREATED"),
    DEVELOPMENT("DEVELOPMENT"),
    SUBMITTED_FOR_REVIEW("SUBMITTED_FOR_REVIEW"),
    APPROVED_BY_GOVERNANCE("APPROVED_BY_GOVERNANCE"),
    REJECTED("REJECTED"),
    STAGING("STAGING"),
    PRODUCTION("PRODUCTION"),
    CANCELED("CANCELED"),
    HIDDEN("HIDDEN");

    private final String value;

    PlanStatus(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PlanStatus fromValue(String text) {
        for (val b : PlanStatus.values()) {
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
