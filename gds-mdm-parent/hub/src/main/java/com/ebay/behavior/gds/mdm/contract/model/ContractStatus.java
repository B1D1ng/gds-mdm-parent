package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.Environment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.val;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;

public enum ContractStatus {
    IN_DEVELOPMENT,
    PENDING_APPROVAL,
    REJECTED,
    ONBOARDING,
    TESTING,
    TESTING_FAILED,
    TESTING_COMPLETE,
    DEPLOYING_TEST,
    DEPLOYING_STAGING,
    DEPLOYING_PRODUCTION,
    DEPLOY_STAGING_FAILED,
    DEPLOY_PRODUCTION_FAILED,
    STAGING_RELEASED,
    RELEASED,
    ARCHIVING,
    STOPPING_TEST,
    ARCHIVED;

    @JsonCreator
    public static ContractStatus fromName(String text) {
        for (val status : ContractStatus.values()) {
            if (status.name().equals(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format("Unexpected value: %s", text));
    }

    @JsonValue
    public String getValue() {
        return name();
    }

    public static ContractStatus getFailureStatus(DeployScope scope, Environment environment) {
        if (scope == DeployScope.TEST) {
            return TESTING_FAILED;
        }
        return environment == PRODUCTION ? DEPLOY_PRODUCTION_FAILED : DEPLOY_STAGING_FAILED;
    }
}
