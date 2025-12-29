package com.ebay.behavior.gds.mdm.dec.model.enums;

public enum Environment {
    UNSTAGED,
    STAGING,
    PRODUCTION;

    public boolean isTransitionAllowedTo(Environment newEnv) {
        return switch (this) {
            case UNSTAGED -> newEnv == STAGING;
            case STAGING -> newEnv == PRODUCTION;
            case PRODUCTION -> false; // No transitions allowed from PRODUCTION
        };
    }
}
