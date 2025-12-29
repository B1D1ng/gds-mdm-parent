package com.ebay.behavior.gds.mdm.signal.util;

import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.signal.common.model.Signal;
import com.ebay.behavior.gds.mdm.signal.common.model.search.PlanSearchBy;

import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.signal.util.ServiceUtils.PREFIX_MIN_LENGTH;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notEmpty;

@UtilityClass
public class ValidationUtils {

    public static void validateSearchPrefix(String searchTerm) {
        isTrue(searchTerm.length() >= PREFIX_MIN_LENGTH, String.format("KeyPrefix length should be at least %d chars", PREFIX_MIN_LENGTH));
    }

    public static SearchCriterion validateSearchPrefix(String searchTerm, String searchCriterion) {
        return validateSearchPrefix(searchTerm, searchCriterion, null);
    }

    public static SearchCriterion validateSearchPrefix(String searchTerm, String searchCriterion, SearchCriterion expectedCriterion) {
        notBlank(searchTerm, "searchTerm must not be blank");
        notBlank(searchCriterion, "searchCriterion must not be blank");
        val minLength = 2;
        isTrue(searchTerm.length() >= minLength, String.format("KeyPrefix length should be at least %d chars", minLength));

        if (nonNull(expectedCriterion)) {
            isTrue(expectedCriterion.getValue().equals(searchCriterion),
                    String.format("Only %s searchCriterion is supported for prefixes", expectedCriterion.getValue()));
        }

        return SearchCriterion.fromValue(searchCriterion);
    }

    public static void validatePlanSearchParams(PlanSearchBy searchBy, String searchTerm, boolean ownedByMe) {
        if (isNull(searchBy)) {
            isTrue(isNull(searchTerm), "searchBy is not null");
            return;
        }

        isTrue(nonNull(searchTerm), "searchTerm is null");

        if (ownedByMe && searchBy.equals(PlanSearchBy.OWNER)) {
            throw new IllegalArgumentException("Filter by Owner not supported in 'Owned by me' tab");
        }
    }

    public static void validateSearchCriterion(SearchCriterion searchCriterion, List<SearchCriterion> expectedCriteria) {
        isTrue(nonNull(searchCriterion), "searchCriterion must not be null");
        notEmpty(expectedCriteria, "expectedCriteria must not be empty");

        if (expectedCriteria.contains(searchCriterion)) {
            return;
        }

        throw new IllegalArgumentException(String.format("SearchCriterion %s not supported", searchCriterion));
    }

    public static void validateCompleted(Signal signal) {
        isTrue(COMPLETED.equals(signal.getCompletionStatus()), "A Signal must be COMPLETED");
    }
}
