package com.ebay.behavior.gds.mdm.commonSvc.service;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.search.Search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.function.BiFunction;

public interface SearchService<M extends Auditable> {

    Page<M> findByTerm(Search search,
                       BiFunction<String, Pageable, Page<M>> findByTermMethod,
                       BiFunction<String, Pageable, Page<M>> findByTermStartingWithMethod,
                       BiFunction<String, Pageable, Page<M>> findByTermContainingMethod);

    Page<M> findByIdTerm(Search search, BiFunction<Long, Pageable, Page<M>> findByIdMethod);
}
