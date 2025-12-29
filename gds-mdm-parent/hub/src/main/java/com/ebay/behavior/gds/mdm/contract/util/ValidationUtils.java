package com.ebay.behavior.gds.mdm.contract.util;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.ContractSourceType;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.search.ContractSearchBy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ForbiddenException;
import lombok.experimental.UtilityClass;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.Validate.isTrue;

@UtilityClass
public class ValidationUtils {

    public static void validateContractSearchParams(ContractSearchBy searchBy, String searchTerm, boolean ownedByMe) {
        if (isNull(searchBy)) {
            isTrue(isNull(searchTerm), "searchBy is not null");
            return;
        }

        isTrue(nonNull(searchTerm), "searchTerm is null");

        if (ownedByMe && searchBy.equals(ContractSearchBy.OWNER)) {
            throw new IllegalArgumentException("Filter by Owner not supported in 'Owned by me' tab");
        }
    }

    public static void validateModerator(String user, ContractGovernanceConfiguration configuration) {
        if (isNull(configuration)) {
            return;
        }
        if (!configuration.isModerator(user)) {
            throw new ForbiddenException("User is not a moderator");
        }
    }

    public static boolean validServerCall(HttpServletRequest request) {
        // TODO integrate TF validation here, ticket: https://jirap.corp.ebay.com/browse/CJS-1953
        return isNotEmpty(request.getHeader("X-EBAY-TF-AUTHORIZATION"));
    }

    public static void validateHiveContract(@NotNull @Valid UnstagedContract contract) {
        isTrue(ContractSourceType.fromContract(contract) == ContractSourceType.HIVE, "Hive contract must have sourceType HIVE");
    }
}
