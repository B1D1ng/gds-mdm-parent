package com.ebay.behavior.gds.mdm.contract.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.ARCHIVING;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.DEPLOYING_PRODUCTION;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.DEPLOYING_STAGING;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.DEPLOYING_TEST;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.IN_DEVELOPMENT;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.ONBOARDING;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.PENDING_APPROVAL;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.REJECTED;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.STOPPING_TEST;

public enum ContractUserAction {
    UPDATE("UPDATE", IN_DEVELOPMENT),
    SUBMIT("SUBMIT",PENDING_APPROVAL),
    APPROVE("APPROVE", ONBOARDING),
    REJECT("REJECT", REJECTED),
    TEST("TEST", DEPLOYING_TEST),
    COMPLETE_TEST("COMPLETE_TEST", STOPPING_TEST),
    DEPLOY_STAGING("DEPLOY_STAGING", DEPLOYING_STAGING),
    DEPLOY_PRODUCTION("DEPLOY_PRODUCTION", DEPLOYING_PRODUCTION),
    ARCHIVE("ARCHIVE", ARCHIVING);

    private final String value;
    @Getter
    private final ContractStatus contractStatus;

    ContractUserAction(String value, ContractStatus contractStatus) {
        this.value = value;
        this.contractStatus = contractStatus;
    }

    @JsonCreator
    public static ContractUserAction fromValue(String text) {
        for (ContractUserAction b : ContractUserAction.values()) {
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