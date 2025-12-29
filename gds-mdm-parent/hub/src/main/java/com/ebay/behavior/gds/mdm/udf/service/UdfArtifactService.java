package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfArtifact;
import com.ebay.behavior.gds.mdm.udf.repository.UdfArtifactRepository;

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
public class UdfArtifactService
        extends AbstractCrudService<UdfArtifact>
        implements CrudService<UdfArtifact>, SearchService<UdfArtifact> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<UdfArtifact> modelType = UdfArtifact.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UdfArtifactRepository repository;

    @Override
    public Page<UdfArtifact> getAll(Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    public UdfArtifact getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }
}
