package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction;
import com.ebay.behavior.gds.mdm.signal.config.GovernanceConfiguration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.UNKNOWN;

@Service
@Validated
public class OwnershipService {

    @Autowired
    private GovernanceConfiguration configuration;

    public void setUserPermissions(@NotNull @Valid Plan plan, @NotBlank String user) {
        final var operations = getUserPermissions(plan, user);
        plan.setOperations(operations.stream().toList());
    }

    public Set<PlanUserAction> getUserPermissions(@NotNull @Valid Plan plan, @NotBlank String user) {
        val operations = new HashSet<PlanUserAction>();

        if (isOwner(plan, user)) {
            operations.addAll(getUserPermissions(plan));
        }

        if (configuration.isModerator(user)) {
            operations.addAll(getModeratorPermissions(plan));
        }
        return operations;
    }

    private List<PlanUserAction> getUserPermissions(Plan plan) {
        return switch (plan.getStatus()) {
            case CREATED, DEVELOPMENT -> List.of(PlanUserAction.COMPLETE, PlanUserAction.CANCEL, PlanUserAction.SUBMIT_FOR_REVIEW);
            case REJECTED -> List.of(PlanUserAction.COMPLETE, PlanUserAction.SUBMIT_FOR_REVIEW, PlanUserAction.CANCEL);
            case STAGING -> List.of(PlanUserAction.PROMOTE_TO_PROD);
            case PRODUCTION, CANCELED, SUBMITTED_FOR_REVIEW, HIDDEN -> List.of();
            case APPROVED_BY_GOVERNANCE -> List.of(PlanUserAction.CANCEL, PlanUserAction.PROMOTE_TO_STAGING);
        };
    }

    private List<PlanUserAction> getModeratorPermissions(Plan plan) {
        return switch (plan.getStatus()) {
            case CREATED, DEVELOPMENT, STAGING -> List.of(PlanUserAction.CANCEL, PlanUserAction.HIDE);
            case PRODUCTION, CANCELED, APPROVED_BY_GOVERNANCE, HIDDEN, REJECTED -> List.of();
            case SUBMITTED_FOR_REVIEW -> List.of(PlanUserAction.CANCEL, PlanUserAction.REJECT, PlanUserAction.APPROVE);
        };
    }

    private boolean isOwner(Plan plan, String user) {
        if (user == null || CollectionUtils.isEmpty(plan.getOwnersAsList()) || UNKNOWN.equals(user)) {
            return false;
        }
        return plan.getOwnersAsList().contains(user);
    }
}
