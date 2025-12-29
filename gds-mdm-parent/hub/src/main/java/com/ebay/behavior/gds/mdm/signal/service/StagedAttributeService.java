package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy;
import com.ebay.behavior.gds.mdm.signal.repository.StagedAttributeRepository;

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
public class StagedAttributeService
        extends AbstractCrudService<StagedAttribute>
        implements CrudService<StagedAttribute>, SearchService<StagedAttribute> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<StagedAttribute> modelType = StagedAttribute.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private StagedAttributeRepository repository;

    @Override
    public StagedAttribute update(@NotNull @Valid StagedAttribute attr) {
        throw new NotImplementedException("Immutable staged attribute");
    }

    @Override
    public void delete(long id) {
        throw new NotImplementedException("Immutable staged field");
    }

    @Override
    @Transactional(readOnly = true)
    public StagedAttribute getByIdWithAssociations(long id) {
        val attribute = getById(id);
        Hibernate.initialize(attribute.getEvent());
        return attribute;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StagedAttribute> getAll(@Valid @NotNull Search search) {
        val searchBy = AttributeSearchBy.valueOf(search.getSearchBy());

        return switch (searchBy) {
            case TAG -> findByTag(search);
            case DESCRIPTION -> findByDescription(search);
        };
    }

    private Page<StagedAttribute> findByTag(Search search) {
        return findByTerm(search, repository::findByTag, repository::findByTagStartingWith, repository::findByTagContaining);
    }

    private Page<StagedAttribute> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }
}