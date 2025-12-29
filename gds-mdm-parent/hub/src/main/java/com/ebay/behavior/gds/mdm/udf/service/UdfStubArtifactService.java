package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubArtifact;
import com.ebay.behavior.gds.mdm.udf.repository.UdfStubArtifactRepository;

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
public class UdfStubArtifactService
        extends AbstractCrudService<UdfStubArtifact>
        implements CrudService<UdfStubArtifact>, SearchService<UdfStubArtifact> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<UdfStubArtifact> modelType = UdfStubArtifact.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UdfStubArtifactRepository repository;

    @Override
    public Page<UdfStubArtifact> getAll(Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    public UdfStubArtifact getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }
}
