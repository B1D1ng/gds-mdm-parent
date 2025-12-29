package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.contract.model.Transformation;
import com.ebay.behavior.gds.mdm.contract.repository.TransformationRepository;

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
public class TransformationService
        extends AbstractCrudService<Transformation> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<Transformation> modelType = Transformation.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private TransformationRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<Transformation> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(readOnly = true)
    public Transformation getByIdWithAssociations(long id) {
        val transformation = getById(id);
        Hibernate.initialize(transformation.getComponent());
        return transformation;
    }
}
