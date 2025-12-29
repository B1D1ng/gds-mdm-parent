package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.contract.model.Filter;
import com.ebay.behavior.gds.mdm.contract.repository.FilterRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class FilterService
        extends AbstractCrudService<Filter> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<Filter> modelType = Filter.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private FilterRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<Filter> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(readOnly = true)
    public Filter getByIdWithAssociations(long id) {
        val filter = getById(id);
        Hibernate.initialize(filter.getComponent());
        return filter;
    }
}