package com.ebay.behavior.gds.mdm.common.model.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;

public enum SearchCriterion {

    EXACT_MATCH("EXACT_MATCH"),
    EXACT_MATCH_IGNORE_CASE("EXACT_MATCH_IGNORE_CASE"),
    STARTS_WITH("STARTS_WITH"),
    STARTS_WITH_IGNORE_CASE("STARTS_WITH_IGNORE_CASE"),
    CONTAINS("CONTAINS"),
    CONTAINS_IGNORE_CASE("CONTAINS_IGNORE_CASE"),
    GREATER_THAN("GREATER_THAN"),
    GREATER_THAN_OR_EQUAL_TO("GREATER_THAN_OR_EQUAL_TO"),
    LESS_THAN("LESS_THAN"),
    LESS_THAN_OR_EQUAL_TO("LESS_THAN_OR_EQUAL_TO"),
    NOT_EQUAL("NOT_EQUAL"),
    NOT_EQUAL_IGNORE_CASE("NOT_EQUAL_IGNORE_CASE");

    private final String value;

    SearchCriterion(String value) {
        this.value = value;
    }

    @JsonCreator
    public static SearchCriterion fromValue(String text) {
        for (val b : SearchCriterion.values()) {
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
