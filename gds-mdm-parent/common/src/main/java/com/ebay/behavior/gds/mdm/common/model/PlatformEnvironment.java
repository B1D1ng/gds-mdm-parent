package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * Source: <a href="https://stackoverflowteams.com/c/ebay/questions/8504/share-all-the-possible-value-of-this-api-com-ebay-raptorio-env-platformenvprope">...</a>
 */
public enum PlatformEnvironment {
    DEV("Dev"),
    QA("QA"),
    LnP("LnP"),
    SANDBOX("Sandbox"),
    PRODUCTION("Production"),
    PRE_PRODUCTION("Pre-Production");

    private final String value;

    PlatformEnvironment(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PlatformEnvironment fromValue(String text) {
        if (StringUtils.isBlank(text)) {
            return DEV;
        }

        for (PlatformEnvironment b : values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }

        return DEV;
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
