package com.ebay.behavior.gds.mdm.common.model.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchBy {

    ID("id"),
    NAME("name"),
    DESCRIPTION("description"),
    OWNERS("owners"),
    CREATE_BY("createBy"),
    UPDATE_BY("updateBy"),
    STATUS("status"),
    DOMAIN("domain"),
    TYPE("type");

    private final String value;

    SearchBy(String value) {
        this.value = value;
    }

    @JsonCreator
    public static SearchBy fromValue(String text) {
        for (SearchBy b : SearchBy.values()) {
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
