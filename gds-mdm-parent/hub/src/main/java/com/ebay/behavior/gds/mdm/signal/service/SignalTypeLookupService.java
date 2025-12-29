package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractLookupService;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTypeLookup;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SignalTypeDimensionMapping;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SignalTypePhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.signal.repository.SignalTypeRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SignalTypeDimensionMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SignalTypePhysicalStorageMappingRepository;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class SignalTypeLookupService extends AbstractLookupService<SignalTypeLookup> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<SignalTypeLookup> modelType = SignalTypeLookup.class;

    @Autowired
    private SignalTypePhysicalStorageMappingRepository mappingRepository;

    @Autowired
    private SignalTypeDimensionMappingRepository dimMappingRepository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private SignalTypeRepository repository;

    @Autowired
    private SignalPhysicalStorageService physicalStorageService;

    @Autowired
    private SignalDimTypeLookupService dimTypeLookupService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SignalTypePhysicalStorageMapping createPhysicalStorageMapping(long lookupId, long storageId) {
        getById(lookupId);
        physicalStorageService.getById(storageId);

        val mapping = new SignalTypePhysicalStorageMapping(lookupId, storageId);
        return mappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deletePhysicalStorageMapping(long lookupId, long storageId) {
        val mapping = mappingRepository
                .findBySignalTypeIdAndPhysicalStorageId(lookupId, storageId)
                .orElseThrow(() -> new DataNotFoundException(
                        SignalTypePhysicalStorageMapping.class, lookupId, storageId));
        mappingRepository.deleteById(mapping.getId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SignalTypeDimensionMapping createDimensionMapping(long lookupId, long dimId, boolean isMandatory) {
        val signalType = getById(lookupId);
        val dimension = dimTypeLookupService.getById(dimId);

        // if not exists, create mapping
        val mapping = new SignalTypeDimensionMapping(signalType, dimension, isMandatory);
        return dimMappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SignalTypeDimensionMapping updateDimensionMapping(long lookupId, long dimId, boolean isMandatory) {
        val mapping = getDimensionMapping(lookupId, dimId);
        mapping.setIsMandatory(isMandatory);
        return dimMappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteDimensionMapping(long lookupId, long dimId) {
        val mapping = getDimensionMapping(lookupId, dimId);
        dimMappingRepository.deleteById(mapping.getId());
    }

    @Transactional(readOnly = true)
    public SignalTypeDimensionMapping getDimensionMapping(long lookupId, long dimId) {
        return dimMappingRepository
                .findBySignalTypeIdAndDimensionId(lookupId, dimId)
                .orElseThrow(() -> new DataNotFoundException(SignalTypeDimensionMapping.class, lookupId, dimId));
    }

    private Boolean getMandatoryValue(Boolean isMandatory) {
        return isMandatory == null || isMandatory;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Set<SignalDimTypeLookup> getDimensionsBySignalTypeId(long signalTypeId) {
        val mappings = dimMappingRepository.findBySignalTypeId(signalTypeId);

        return mappings.stream()
                .map(SignalTypeDimensionMapping::getDimension)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SignalDimTypeLookup::getId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
