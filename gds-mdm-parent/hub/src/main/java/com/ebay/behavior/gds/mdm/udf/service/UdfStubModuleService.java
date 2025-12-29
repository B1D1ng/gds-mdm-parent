package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubModule;
import com.ebay.behavior.gds.mdm.udf.repository.UdfStubModuleRepository;

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
public class UdfStubModuleService
        extends AbstractCrudService<UdfStubModule>
        implements CrudService<UdfStubModule>, SearchService<UdfStubModule> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<UdfStubModule> modelType = UdfStubModule.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UdfStubModuleRepository repository;

    @Override
    public Page<UdfStubModule> getAll(Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    public UdfStubModule getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }
}
