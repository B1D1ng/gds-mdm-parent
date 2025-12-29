package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.contract.model.ConfigStorageMapping;
import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;
import com.ebay.behavior.gds.mdm.contract.repository.ConfigStorageMappingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveConfigRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveSourceRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveStorageRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;

@Validated
@Service
public class HiveConfigService extends AbstractCrudService<HiveConfig> {

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private HiveConfigRepository repository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private ConfigStorageMappingRepository mappingRepository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private HiveSourceRepository hiveSourceRepository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private HiveStorageRepository storageRepository;

    @Override
    protected JpaRepository<HiveConfig, Long> getRepository() {
        return repository;
    }

    @Override
    protected Class<HiveConfig> getModelType() {
        return HiveConfig.class;
    }

    @Override
    public Page<HiveConfig> getAll(Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(readOnly = true)
    public HiveConfig getByIdWithAssociations(long id) {
        val hiveConfig = getById(id);
        Hibernate.initialize(hiveConfig.getHiveStorage());
        return hiveConfig;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateMapping(@PositiveOrZero Long configId, @PositiveOrZero Long storageId) {
        getById(configId);
        storageRepository.findById(storageId).orElseThrow(() ->
                new IllegalArgumentException("Hive Storage with id " + storageId + " does not exist."));
        try {
            mappingRepository.save(new ConfigStorageMapping(configId, storageId));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Failed to create mapping between HiveConfig " + configId
                    + " and HiveStorage " + storageId + ". Duplicated key: " + ex.getMessage(), ex);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public HiveConfig update(@NotNull @Valid HiveConfig model) {
        validateForUpdate(model);
        getById(model.getId()); // Ensure signal exists before update
        val componentId = model.getComponentId();
        hiveSourceRepository.findById(componentId).orElseThrow(() ->
                new IllegalArgumentException("Hive Source with id " + componentId + " does not exist."));

        return getRepository().save(model);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void delete(long id) {
        findById(id);
        getRepository().deleteById(id);
    }
}