package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.repository.ConfigStorageMappingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveConfigRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveSourceRepository;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HiveSourceService extends AbstractComponentService<HiveSource> {

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private HiveSourceRepository repository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private HiveConfigRepository configRepository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private ConfigStorageMappingRepository mappingRepository;

    @Override
    protected JpaRepository<HiveSource, Long> getRepository() {
        return repository;
    }

    @Override
    protected Class<HiveSource> getModelType() {
        return HiveSource.class;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id);

        val routings = getRoutings(id);

        if (!routings.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete component with associated routings.");
        }
        val hiveConfigs = configRepository.getHiveConfigByComponentId(id);
        if (!hiveConfigs.isEmpty()) {
            hiveConfigs.forEach(hiveConfig -> {
                val mapping = mappingRepository.findByConfigId(hiveConfig.getId());
                if (mapping != null) {
                    mappingRepository.deleteById(mapping.getId());
                }
            });
            configRepository.deleteAllByComponentId(id);
        }
        getRepository().deleteById(id);
    }
}
