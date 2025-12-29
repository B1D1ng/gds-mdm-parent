package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.ContractUserAction;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.UNKNOWN;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Validated
public class ContractOwnershipService {

    @Autowired
    private ContractGovernanceConfiguration configuration;

    public void setUserPermissions(@NotNull @Valid UnstagedContract contract, @NotBlank String user) {
        contract.setOperations(getUserPermissions(contract, user));
    }

    public List<ContractUserAction> getUserPermissions(@NotNull @Valid UnstagedContract contract, @NotBlank String user) {
        val operations = new HashSet<ContractUserAction>();

        if (isOwner(contract, user)) {
            operations.addAll(getOwnerPermissions(contract));
        }

        if (configuration.getModerators().contains(user)) {
            operations.addAll(getModeratorPermissions(contract));
        }

        return new ArrayList<>(operations);
    }

    private List<ContractUserAction> getOwnerPermissions(@NotNull @Valid UnstagedContract contract) {
        return switch (contract.getStatus()) {
            case IN_DEVELOPMENT, REJECTED -> List.of(ContractUserAction.UPDATE, ContractUserAction.SUBMIT, ContractUserAction.ARCHIVE);
            default -> List.of();
        };
    }

    private List<ContractUserAction> getModeratorPermissions(UnstagedContract contract) {
        return switch (contract.getStatus()) {
            case IN_DEVELOPMENT, REJECTED -> List.of(ContractUserAction.UPDATE, ContractUserAction.SUBMIT, ContractUserAction.ARCHIVE);
            case PENDING_APPROVAL -> List.of(ContractUserAction.APPROVE, ContractUserAction.REJECT);
            case ONBOARDING -> List.of(ContractUserAction.TEST, ContractUserAction.UPDATE, ContractUserAction.ARCHIVE);
            case TESTING -> List.of(ContractUserAction.UPDATE, ContractUserAction.TEST, ContractUserAction.COMPLETE_TEST, ContractUserAction.ARCHIVE);
            case TESTING_FAILED -> List.of(ContractUserAction.UPDATE, ContractUserAction.TEST, ContractUserAction.ARCHIVE);
            case TESTING_COMPLETE -> List.of(ContractUserAction.UPDATE, ContractUserAction.DEPLOY_STAGING, ContractUserAction.ARCHIVE, ContractUserAction.TEST);
            case DEPLOY_STAGING_FAILED -> List.of(ContractUserAction.UPDATE, ContractUserAction.DEPLOY_STAGING, ContractUserAction.TEST);
            case DEPLOY_PRODUCTION_FAILED -> List.of(ContractUserAction.UPDATE, ContractUserAction.DEPLOY_STAGING, ContractUserAction.DEPLOY_PRODUCTION);
            case STAGING_RELEASED -> List.of(ContractUserAction.UPDATE, ContractUserAction.ARCHIVE, ContractUserAction.DEPLOY_PRODUCTION,
                    ContractUserAction.TEST);
            case RELEASED -> List.of(ContractUserAction.UPDATE, ContractUserAction.ARCHIVE);
            default -> List.of();
        };
    }

    private boolean isOwner(UnstagedContract contract, String user) {
        if (isBlank(user) || CollectionUtils.isEmpty(contract.getOwnersAsList()) || UNKNOWN.equals(user)) {
            return false;
        }
        return contract.getOwnersAsList().contains(user);
    }
}