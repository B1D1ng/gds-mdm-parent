package com.ebay.behavior.gds.mdm.dec.model.enums;

public enum DecEnvironment {
    DEV_FACING_STAGING,
    DEV_FACING_PRE_PRODUCTION,
    STAGING,
    PRODUCTION,
    PRE_PRODUCTION,
    SDDZ;

    public static boolean isValid(String value) {
        for (DecEnvironment env : DecEnvironment.values()) {
            if (env.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
