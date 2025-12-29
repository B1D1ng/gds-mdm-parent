package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request;

import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

@Value
public class TmsSearchRequest {

    @NotBlank
    private String bestMatch;

    @NotNull
    private SearchIn searchIn;

    @NotNull
    private SearchCr criteria;

    public TmsSearchRequest(String searchTerm, String searchBy, SearchCriterion searchCriterion) {
        bestMatch = searchTerm;
        searchIn = SearchIn.fromValue(searchBy);
        criteria = SearchCr.fromValue(searchCriterion);

        Validate.isTrue(Objects.nonNull(searchIn), String.format("Unsupported searchBy: %s", searchBy));
        Validate.isTrue(Objects.nonNull(criteria), String.format("Unsupported searchCriterion: %s", searchCriterion));
    }
}
