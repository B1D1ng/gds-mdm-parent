package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy;
import com.ebay.behavior.gds.mdm.signal.repository.AttributeTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.FieldAttributeTemplateMappingRepository;

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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class AttributeTemplateService
        extends AbstractCrudService<AttributeTemplate>
        implements CrudService<AttributeTemplate>, SearchService<AttributeTemplate> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<AttributeTemplate> modelType = AttributeTemplate.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private AttributeTemplateRepository repository;

    @Autowired
    private FieldAttributeTemplateMappingRepository mappingRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AttributeTemplate update(@NotNull @Valid AttributeTemplate attribute) {
        throw new NotImplementedException("AttributeTemplate cannot be updated by design. Delete and create new instead");
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id);

        if (!mappingRepository.findByAttributeId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete AttributeTemplate with associated FieldTemplate(s)");
        }

        super.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeTemplate getByIdWithAssociations(long id) {
        val attribute = getById(id);
        Hibernate.initialize(attribute.getEvent());
        return attribute;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttributeTemplate> getAll(@Valid @NotNull Search search) {
        val searchBy = AttributeSearchBy.valueOf(search.getSearchBy());

        return switch (searchBy) {
            case TAG -> findByTag(search);
            case DESCRIPTION -> findByDescription(search);
        };
    }

    private Page<AttributeTemplate> findByTag(Search search) {
        return findByTerm(search, repository::findByTag, repository::findByTagStartingWith, repository::findByTagContaining);
    }

    private Page<AttributeTemplate> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }
}