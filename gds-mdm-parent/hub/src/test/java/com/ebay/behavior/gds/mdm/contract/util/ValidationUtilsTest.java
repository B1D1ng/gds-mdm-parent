package com.ebay.behavior.gds.mdm.contract.util;

import com.ebay.behavior.gds.mdm.contract.model.search.ContractSearchBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateContractSearchParams;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ValidationUtilsTest {

    @Test
    void validateContractSearchParams_searchByNullTermNotNull_error() {
        assertThatThrownBy(() -> validateContractSearchParams(null, "test", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("searchBy is not null");
    }

    @Test
    void validateContractSearchParams_searchByNotNullTermNull_error() {
        assertThatThrownBy(() -> validateContractSearchParams(ContractSearchBy.CONTRACT, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("searchTerm is null");
    }

    @Test
    void validateContractSearchParams_invalidSearchCriterion_error() {
        assertThatThrownBy(() -> validateContractSearchParams(ContractSearchBy.OWNER, "test", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filter by Owner not supported in 'Owned by me' tab");
    }
}