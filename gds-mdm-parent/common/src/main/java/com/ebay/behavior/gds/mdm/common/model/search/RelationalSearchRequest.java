package com.ebay.behavior.gds.mdm.common.model.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationalSearchRequest {

    @NotNull
    @Positive
    private Integer pageSize;

    @NotNull
    @PositiveOrZero
    private Integer pageNumber;

    @Valid
    private SortRequest sort;

    private List<@Valid @NotNull Filter> filters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filter {
        @NotBlank
        private String field;

        @NotNull
        private SearchCriterion operator;

        @NotBlank
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortRequest {
        @NotBlank
        private String field;

        @NotNull
        private Sort.Direction direction;
    }
}