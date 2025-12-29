package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldPhysicalStorageMappingRepository;

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
public class LdmFieldPhysicalStorageMappingService extends AbstractCrudService<LdmFieldPhysicalStorageMapping> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<LdmFieldPhysicalStorageMapping> modelType = LdmFieldPhysicalStorageMapping.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmFieldPhysicalStorageMappingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<LdmFieldPhysicalStorageMapping> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<LdmFieldPhysicalStorageMapping> getAll() {
        return repository.findAll();
    }

    @Override
    public LdmFieldPhysicalStorageMapping getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }
}
