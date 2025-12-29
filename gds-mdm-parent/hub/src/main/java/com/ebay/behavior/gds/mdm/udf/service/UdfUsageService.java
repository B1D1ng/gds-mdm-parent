package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfUsage;
import com.ebay.behavior.gds.mdm.udf.repository.UdfUsageRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
public class UdfUsageService
        extends AbstractCrudService<UdfUsage>
        implements CrudService<UdfUsage>, SearchService<UdfUsage> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<UdfUsage> modelType = UdfUsage.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UdfUsageRepository repository;

    @Override
    public Page<UdfUsage> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(readOnly = true)
    public UdfUsage getByIdWithAssociations(long id) {
        var udfUsage = getById(id);
        Hibernate.initialize(udfUsage.getUdf());
        return udfUsage;
    }
}
