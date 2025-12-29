package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.repository.ConfigStorageMappingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveStorageRepository;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@Validated
public class HiveStorageService extends AbstractCrudService<HiveStorage> {

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private HiveStorageRepository repository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private ConfigStorageMappingRepository mappingRepository;

    @Override
    protected JpaRepository<HiveStorage, Long> getRepository() {
        return repository;
    }

    @Override
    protected Class<HiveStorage> getModelType() {
        return HiveStorage.class;
    }

    @Override
    public Page<HiveStorage> getAll(Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(readOnly = true)
    public HiveStorage getByIdWithAssociations(long id) {
        return getById(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void delete(long id) {
        getById(id);
        val mapping = mappingRepository.findByStorageId(id);
        Optional.ofNullable(mapping).ifPresent(m -> {
            throw new IllegalArgumentException("Cannot delete HiveStorage with associated ConfigStorageMapping id: " + m.getId());
        });
        repository.deleteById(id);
    }
}