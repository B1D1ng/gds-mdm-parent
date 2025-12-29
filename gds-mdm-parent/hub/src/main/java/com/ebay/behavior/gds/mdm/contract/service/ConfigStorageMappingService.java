package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.contract.model.ConfigStorageMapping;
import com.ebay.behavior.gds.mdm.contract.repository.ConfigStorageMappingRepository;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
public class ConfigStorageMappingService extends AbstractCrudService<ConfigStorageMapping> {

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private ConfigStorageMappingRepository repository;

    @Override
    protected Class<ConfigStorageMapping> getModelType() {
        return ConfigStorageMapping.class;
    }

    @Override
    public Page<ConfigStorageMapping> getAll(Search search) {
        throw new UnsupportedOperationException("Not need to getAll for ConfigStorageMapping ");
    }

    @Override
    public ConfigStorageMapping getByIdWithAssociations(long id) {
        throw new UnsupportedOperationException("Not need to getByIdWithAssociations for ConfigStorageMapping");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void delete(long id) {
        findById(id);
        getRepository().deleteById(id);
    }
}