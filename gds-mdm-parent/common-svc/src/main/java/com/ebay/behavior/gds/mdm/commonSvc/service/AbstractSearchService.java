package com.ebay.behavior.gds.mdm.commonSvc.service;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils;

import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractSearchService<M extends Auditable> implements SearchService<M> {

    @Override
    public Page<M> findByTerm(Search search,
                              BiFunction<String, Pageable, Page<M>> findByTermMethod,
                              BiFunction<String, Pageable, Page<M>> findByTermStartingWithMethod,
                              BiFunction<String, Pageable, Page<M>> findByTermContainingMethod) {
        val searchTerm = search.getSearchTerm();
        val searchCriterion = search.getSearchCriterion();
        val pageable = search.getPageable();

        return switch (searchCriterion) {
            case EXACT_MATCH, EXACT_MATCH_IGNORE_CASE -> findByTermMethod.apply(searchTerm, pageable);
            case STARTS_WITH, STARTS_WITH_IGNORE_CASE -> findByTermStartingWithMethod.apply(searchTerm, pageable);
            case CONTAINS, CONTAINS_IGNORE_CASE -> findByTermContainingMethod.apply(searchTerm, pageable);
            default -> throw searchCriterionNotSupported(searchCriterion);
        };
    }

    @Override
    public Page<M> findByIdTerm(Search search, BiFunction<Long, Pageable, Page<M>> findByIdMethod) {
        val id = search.getSearchTerm();
        CommonValidationUtils.validateId(id);
        return findByNumericTerm(search, Long::parseLong, findByIdMethod);
    }

    private <N extends Number> Page<M> findByNumericTerm(Search search,
                                                         Function<String, N> converterMethod,
                                                         BiFunction<N, Pageable, Page<M>> findByTermMethod) {
        val searchTerm = search.getSearchTerm();
        val number = search.getSearchTerm();
        CommonValidationUtils.validateNumeric(number);

        val searchCriterion = search.getSearchCriterion();
        val pageable = search.getPageable();

        if (searchCriterion == SearchCriterion.EXACT_MATCH) {
            return findByTermMethod.apply(converterMethod.apply(searchTerm), pageable);
        } else {
            throw searchCriterionNotSupported(searchCriterion);
        }
    }

    private NotImplementedException searchCriterionNotSupported(SearchCriterion searchCriterion) {
        return new NotImplementedException(String.format("Search criterion %s not supported", searchCriterion.getValue()));
    }
}
