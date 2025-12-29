package com.ebay.behavior.gds.mdm.dec.model.enums;

public enum PlatformEnvironment {
    STAGING,
    PRE_PRODUCTION,
    PRODUCTION;

    public static boolean isValid(String value) {
        for (PlatformEnvironment env : PlatformEnvironment.values()) {
            if (env.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
