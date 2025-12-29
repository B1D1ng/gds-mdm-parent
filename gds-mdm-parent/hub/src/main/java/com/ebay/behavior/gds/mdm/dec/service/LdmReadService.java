package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldSignalMapping;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmEntityWrapper;
import com.ebay.behavior.gds.mdm.dec.model.dto.SignalStorage;
import com.ebay.behavior.gds.mdm.dec.model.dto.StorageDetail;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.SignalStorageType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.repository.LdmBaseEntityRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalStorageRepository;
import com.ebay.behavior.gds.mdm.dec.util.EntityUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;
import com.ebay.behavior.gds.mdm.signal.service.SignalPhysicalStorageService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.dec.util.EntityUtils.excludeTextFields;
import static org.hibernate.Hibernate.initialize;

@Slf4j
@Service
@Validated
@SuppressWarnings("PMD.GodClass")
public class LdmReadService {

    @Autowired
    private SignalPhysicalStorageService signalStorageService;

    @Autowired
    private StagedSignalService stagedSignalService;

    @Autowired
    private LdmEntityRepository repository;

    @Autowired
    private LdmBaseEntityRepository baseEntityRepository;

    @Autowired
    private PhysicalStorageRepository physicalStorageRepository;

    @Autowired
    private LdmFieldPhysicalStorageMappingRepository fieldPhysicalMappingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<LdmEntity> getAllCurrentVersion() {
        return getAllCurrentVersion(false);
    }

    @Transactional(readOnly = true)
    public List<LdmEntity> getAllCurrentVersion(Boolean excludeTextFields) {
        val ldmEntities = repository.findAllCurrentVersion();
        if (excludeTextFields != null && excludeTextFields) {
            for (val ldmEntity : ldmEntities) {
                excludeTextFields(ldmEntity);
            }
        }
        return ldmEntities;
    }

    @Transactional(readOnly = true)
    public LdmEntity getByIdCurrentVersion(@NotNull Long id) {
        return repository.findByIdCurrentVersion(id).orElseThrow(() -> new DataNotFoundException(LdmEntity.class, id));
    }

    @Transactional(readOnly = true)
    public List<LdmEntity> getByEntityIdWithAssociations(@NotNull Long entityId, String env) {
        var ldms = repository.findByEntityIdCurrentVersion(entityId);
        ldms.forEach(ldm -> {
            initializeFields(ldm.getFields(), env);
            // Sort fields by ordinal
            ldm.setFields(EntityUtils.getSortedFieldsByOrdinal(ldm.getFields()));
            // Initialize errorHandlingStorageMappings to prevent lazy initialization exceptions
            initialize(ldm.getErrorHandlingStorageMappings());
        });
        return ldms;
    }

    @Transactional(readOnly = true)
    public List<LdmEntity> getByEntityId(@NotNull Long entityId) {
        return repository.findByEntityIdCurrentVersion(entityId);
    }

    @Transactional(readOnly = true)
    public LdmEntity getByIdWithAssociations(@Valid @NotNull VersionedId id, String env) {
        val entity = repository.findById(id).orElseThrow(() -> new DataNotFoundException(LdmEntity.class, String.valueOf(id)));
        initializeAssociations(entity, env);
        return entity;
    }

    private void initializeAssociations(LdmEntity entity, String env) {
        val baseEntity = baseEntityRepository.findById(entity.getBaseEntityId())
                .orElseThrow(() -> new DataNotFoundException(LdmBaseEntity.class, String.valueOf(entity.getBaseEntityId())));
        entity.setBaseEntity(baseEntity);
        EntityUtils.copyBasicInfoFromBaseEntity(entity, baseEntity);
        initializeFields(entity.getFields(), env);
        entity.setFields(EntityUtils.getSortedFieldsByOrdinal(entity.getFields()));
        // Initialize errorHandlingStorageMappings to prevent lazy initialization exceptions
        initialize(entity.getErrorHandlingStorageMappings());

        List<LdmEntity> dcsEntities = repository.findDcsByUpstreamLdmId(entity.getId());
        Set<Long> dcsLdmIds = dcsEntities.stream()
                .map(LdmEntity::getId)
                .collect(Collectors.toSet());
        entity.setDcsLdms(dcsLdmIds);
    }

    @Transactional(readOnly = true)
    public LdmEntity getByIdWithAssociationsCurrentVersion(@NotNull Long id) {
        return getByIdWithAssociationsCurrentVersion(id, null);
    }

    @Transactional(readOnly = true)
    public LdmEntity getByIdWithAssociationsCurrentVersion(@NotNull Long id, String env) {
        val entity = getByIdCurrentVersion(id);
        initializeAssociations(entity, env);
        return entity;
    }

    private void initializeFields(@NotNull Set<@Valid @NotNull LdmField> fields, String env) {
        initialize(fields);
        for (LdmField field : fields) {
            initialize(field.getSignalMapping());
            val physicalMapping = getLdmFieldPhysicalMapping(field.getId(), env);
            field.setPhysicalStorageMapping(new HashSet<>(physicalMapping));
        }
    }

    private List<LdmFieldPhysicalStorageMapping> getLdmFieldPhysicalMapping(@NotNull Long fieldId, String env) {
        val physicalMapping = fieldPhysicalMappingRepository.findByLdmFieldId(fieldId);
        if (physicalMapping == null) {
            return List.of();
        }
        if (env == null) {
            return physicalMapping;
        }
        val platformEnvironment = PlatformEnvironment.valueOf(env.toUpperCase(Locale.US));
        return physicalMapping.stream().filter(m -> {
            val storageId = m.getPhysicalStorageId();
            val storage = physicalStorageRepository.findById(storageId).orElseThrow(() -> new DataNotFoundException(PhysicalStorage.class, storageId));
            return storage.getStorageEnvironment().equals(platformEnvironment);
        }).toList();
    }

    @Transactional(readOnly = true)
    public LdmEntityWrapper getByIdInWrapper(@NotNull Long id, Boolean withExtendedInfo, String env) {
        LdmEntity entity = getByIdWithAssociationsCurrentVersion(id, env);
        return getLdmEntityWrapper(entity, withExtendedInfo);
    }

    @Transactional(readOnly = true)
    public LdmEntityWrapper getByIdInWrapper(@Valid @NotNull VersionedId id, Boolean withExtendedInfo, String env) {
        LdmEntity entity = getByIdWithAssociations(id, env);
        return getLdmEntityWrapper(entity, withExtendedInfo);
    }

    @Transactional(readOnly = true)
    public LdmEntityWrapper getLdmEntityWrapper(@Valid @NotNull LdmEntity ldmEntity, Boolean withExtendedInfo) {
        // Ensure fields are sorted by ordinal
        Set<LdmField> fields = EntityUtils.getSortedFieldsByOrdinal(ldmEntity.getFields());
        HashMap<Long, Set<Integer>> signalDefinitionIdToVersions = new HashMap<>();
        if (withExtendedInfo != null && withExtendedInfo && fields != null && !fields.isEmpty()) {
            List<LdmFieldPhysicalStorageMapping> mappings = new ArrayList<>();
            for (LdmField field : fields) {
                if (field.getPhysicalStorageMapping() != null) {
                    mappings.addAll(field.getPhysicalStorageMapping());
                }
                if (field.getSignalMapping() != null) {
                    for (LdmFieldSignalMapping mapping : field.getSignalMapping()) {
                        Long signalDefinitionId = mapping.getSignalDefinitionId();
                        Integer signalVersion = mapping.getSignalVersion();
                        signalDefinitionIdToVersions.computeIfAbsent(signalDefinitionId, k -> new HashSet<>()).add(signalVersion);
                    }
                }
            }
            List<SignalStorage> signalStorages = getSignalStorages(signalDefinitionIdToVersions);
            List<Long> physicalStorageIds = mappings.stream().map(LdmFieldPhysicalStorageMapping::getPhysicalStorageId).toList();
            List<PhysicalStorage> physicalStorages = physicalStorageRepository.findAllById(physicalStorageIds);

            return LdmEntityWrapper.builder()
                    .entity(ldmEntity)
                    .physicalStorages(physicalStorages)
                    .signalStorages(signalStorages)
                    .build();
        }
        return LdmEntityWrapper.builder().entity(ldmEntity).build();
    }

    private List<SignalStorage> getSignalStorages(Map<Long, Set<Integer>> signalDefinitionIdToVersions) {
        List<SignalStorage> signalStorages = new ArrayList<>();
        for (Map.Entry<Long, Set<Integer>> entry : signalDefinitionIdToVersions.entrySet()) {
            Long signalDefinitionId = entry.getKey();
            //Set Environment to STAGING, it will pick up the latest version across all environments
            Integer latestVersion = stagedSignalService.getLatestVersionById(signalDefinitionId, Environment.STAGING).getVersion();
            log.info("The latest version for signalDefinitionId {} is {}", signalDefinitionId, latestVersion);
            SignalStorage signalStorage = getSignalStorage(signalDefinitionId, latestVersion);
            signalStorages.add(signalStorage);
        }
        return signalStorages;
    }

    private SignalStorage getSignalStorage(Long signalDefinitionId, Integer latestVersion) {
        try {
            SignalPhysicalStorage physicalStorage = signalStorageService.getBySignalId(VersionedId.of(signalDefinitionId, latestVersion));
            List<StorageDetail> storageDetails = new ArrayList<>();
            if (StringUtils.isNotEmpty(physicalStorage.getKafkaTopic())) {
                StorageDetail kafkaStorageDetail = new StorageDetail(
                        SignalStorageType.STREAM,
                        physicalStorage.getKafkaTopic(),
                        physicalStorage.getKafkaSchema(),
                        null,
                        null
                );
                storageDetails.add(kafkaStorageDetail);
            }

            if (StringUtils.isNotEmpty(physicalStorage.getHiveTableName())) {
                StorageDetail hiveStorageDetail = new StorageDetail(
                        SignalStorageType.TABLE,
                        physicalStorage.getHiveTableName(),
                        null,
                        physicalStorage.getDoneFilePath(),
                        null
                );
                storageDetails.add(hiveStorageDetail);
            }

            return SignalStorage.builder().signalDefinitionId(signalDefinitionId.toString()).storageDetails(storageDetails).build();
        } catch (DataNotFoundException e) {
            log.warn("SignalPhysicalStorage not found for signalId: {}, version: {}", signalDefinitionId, latestVersion);
            return SignalStorage.builder().signalDefinitionId(signalDefinitionId.toString()).storageDetails(null).build();
        }
    }

    @Transactional(readOnly = true)
    public List<LdmEntity> searchByNameAndNamespace(String name, String viewType, String namespaceName) {
        return searchByNameAndNamespace(name, viewType, namespaceName, false);
    }

    @Transactional(readOnly = true)
    public List<LdmEntity> searchByNameAndNamespace(String name, String viewType, String namespaceName, Boolean excludeTextFields) {
        List<LdmEntity> ldmEntities;
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(viewType) && StringUtils.isNotBlank(namespaceName)) { // to deprecate
            ViewType viewTypeEnum = ViewType.valueOf(viewType.toUpperCase(Locale.US));
            ldmEntities = repository.findAllByNameAndTypeAndNamespaceCurrentVersion(name, viewTypeEnum, namespaceName);
        } else if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(viewType)) { // to deprecate
            ViewType viewTypeEnum = ViewType.valueOf(viewType.toUpperCase(Locale.US));
            ldmEntities = repository.findAllByNameAndTypeCurrentVersion(name, viewTypeEnum);
        } else if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(namespaceName)) {
            ldmEntities = repository.findAllByNameAndNamespaceCurrentVersion(name, namespaceName);
        } else if (StringUtils.isNotBlank(name)) {
            ldmEntities = repository.findAllByNameCurrentVersion(name);
        } else {
            ldmEntities = repository.findAllByNamespaceNameCurrentVersion(namespaceName);
        }
        if (excludeTextFields != null && excludeTextFields) {
            for (val ldmEntity : ldmEntities) {
                excludeTextFields(ldmEntity);
            }
        }
        return ldmEntities;
    }
}
