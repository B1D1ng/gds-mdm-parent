package com.ebay.behavior.gds.mdm.udf.common.model.enums;

public enum FunctionSourceType {
    BUILT_IN_FUNC("BUILT_IN_FUNC"),
    USER_DEFINED_FUNC("USER_DEFINED_FUNC"),
    VALUE_FUNC("VALUE_FUNC")
    ;

    private final String value;

    FunctionSourceType(String value) {
        this.value = value;
    }

    public static FunctionSourceType fromValue(String text) {
        for (FunctionSourceType b : FunctionSourceType.values()) {
            if (String.valueOf(b.value).equals(String.valueOf(text))) {
                return b;
            }
        }
        throw new IllegalArgumentException(String.format("Unexpected value: %s", text));
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
