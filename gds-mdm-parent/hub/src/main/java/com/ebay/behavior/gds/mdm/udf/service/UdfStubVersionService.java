package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubVersions;
import com.ebay.behavior.gds.mdm.udf.repository.UdfStubVersionRepository;

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
public class UdfStubVersionService
        extends AbstractCrudService<UdfStubVersions>
        implements CrudService<UdfStubVersions>, SearchService<UdfStubVersions> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<UdfStubVersions> modelType = UdfStubVersions.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UdfStubVersionRepository repository;

    @Override
    public Page<UdfStubVersions> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    public UdfStubVersions getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }
}
