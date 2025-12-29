package com.ebay.behavior.gds.mdm.signal.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.APPROVED_BY_GOVERNANCE;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.CANCELED;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.DEVELOPMENT;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.HIDDEN;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.PRODUCTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.REJECTED;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.STAGING;

public enum PlanUserAction {

    COMPLETE("COMPLETE", DEVELOPMENT),
    SUBMIT_FOR_REVIEW("SUBMIT_FOR_REVIEW", PlanStatus.SUBMITTED_FOR_REVIEW),
    REJECT("REJECT", REJECTED),
    APPROVE("APPROVE", APPROVED_BY_GOVERNANCE),
    PROMOTE_TO_STAGING("PROMOTE_TO_STAGING", STAGING),
    PROMOTE_TO_PROD("PROMOTE_TO_PROD", PRODUCTION),
    CANCEL("CANCEL", CANCELED),
    HIDE("HIDE", HIDDEN);
    private final String value;
    private final PlanStatus planStatus;

    PlanUserAction(String value, PlanStatus planStatus) {
        this.value = value;
        this.planStatus = planStatus;
    }

    @JsonCreator
    public static PlanUserAction fromValue(String text) {
        for (PlanUserAction b : PlanUserAction.values()) {
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

    public PlanStatus getPlanStatus() {
        return planStatus;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
