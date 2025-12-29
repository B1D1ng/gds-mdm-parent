package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request;

import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchCr {

    CONTAINS("contains"),
    EXACT_MATCH("exact match"),
    STARTS_WITH("starts with");

    private final String value;

    SearchCr(String value) {
        this.value = value;
    }

    @JsonCreator
    public static SearchCr fromValue(String text) {
        for (SearchCr b : SearchCr.values()) {
            if (String.valueOf(b.value).equals(String.valueOf(text))) {
                return b;
            }
        }
        throw new IllegalArgumentException(String.format("Unexpected value: %s", text));
    }

    public static SearchCr fromValue(SearchCriterion criterion) {
        return switch (criterion) {
            case CONTAINS -> CONTAINS;
            case EXACT_MATCH -> EXACT_MATCH;
            case STARTS_WITH -> STARTS_WITH;
            default -> null;
        };
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
