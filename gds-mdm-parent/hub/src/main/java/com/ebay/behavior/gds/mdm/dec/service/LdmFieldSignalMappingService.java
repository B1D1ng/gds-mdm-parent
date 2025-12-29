package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldSignalMapping;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldSignalMappingRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class LdmFieldSignalMappingService extends AbstractCrudService<LdmFieldSignalMapping> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<LdmFieldSignalMapping> modelType = LdmFieldSignalMapping.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmFieldSignalMappingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<LdmFieldSignalMapping> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<LdmFieldSignalMapping> getAll() {
        return repository.findAll();
    }

    @Override
    public LdmFieldSignalMapping getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }
}
