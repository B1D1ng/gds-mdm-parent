package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfVersions;
import com.ebay.behavior.gds.mdm.udf.repository.UdfVersionRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
public class UdfVersionService
        extends AbstractCrudService<UdfVersions>
        implements CrudService<UdfVersions>, SearchService<UdfVersions> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<UdfVersions> modelType = UdfVersions.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UdfVersionRepository repository;

    @Override
    public Page<UdfVersions> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    public UdfVersions getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }
}
