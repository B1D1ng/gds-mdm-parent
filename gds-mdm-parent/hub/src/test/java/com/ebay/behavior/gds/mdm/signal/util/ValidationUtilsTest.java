package com.ebay.behavior.gds.mdm.signal.util;

import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.signal.common.model.search.PlanSearchBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.signal.util.ValidationUtils.validatePlanSearchParams;
import static com.ebay.behavior.gds.mdm.signal.util.ValidationUtils.validateSearchCriterion;
import static com.ebay.behavior.gds.mdm.signal.util.ValidationUtils.validateSearchPrefix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ValidationUtilsTest {

    @Test
    void validateSearchPrefix_searchTermShort_error() {
        assertThatThrownBy(() -> validateSearchPrefix("a", EXACT_MATCH.getValue()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KeyPrefix length should be at least");
    }

    @Test
    void validateSearchPrefix_searchTermLongEnough() {
        SearchCriterion result = validateSearchPrefix("ab", EXACT_MATCH.getValue());
        assertThat(result).isEqualTo(EXACT_MATCH);
    }

    @Test
    void validateSearchPrefix_invalidSearchCriterion_error() {
        assertThatThrownBy(() -> validateSearchPrefix("ab", "INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID");
    }

    @Test
    void validateSearchPrefix_expectedCriterionNotNull_searchCriterionDoesNotMatch_error() {
        assertThatThrownBy(() -> validateSearchPrefix("ab", STARTS_WITH.getValue(), EXACT_MATCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only EXACT_MATCH searchCriterion is supported for prefixes");
    }

    @Test
    void validatePlanSearchParams_searchArgumentsAreNull() {
        assertThatCode(() -> validatePlanSearchParams(null, null, true))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePlanSearchParams_okFlow() {
        assertThatCode(() -> validatePlanSearchParams(PlanSearchBy.PLAN, SEARCH_TERM, true))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSearchCriterion_() {
        var criteria = Arrays.asList(EXACT_MATCH, CONTAINS);

        validateSearchCriterion(EXACT_MATCH, criteria);
    }

    @Test
    void validateSearchCriterion_unsupportedCriterion() {
        var criteria = Arrays.asList(EXACT_MATCH, CONTAINS);

        assertThatThrownBy(() -> validateSearchCriterion(STARTS_WITH, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STARTS_WITH not supported");
    }
}