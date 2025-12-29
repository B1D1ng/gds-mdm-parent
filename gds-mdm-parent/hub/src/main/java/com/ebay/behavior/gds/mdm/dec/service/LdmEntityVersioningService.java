package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmErrorHandlingStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Service for handling LDM entity versioning operations.
 */
@Slf4j
@Service
@Validated
public class LdmEntityVersioningService {

    @Autowired
    private LdmEntityRepository repository;

    @Autowired
    private LdmEntityIndexService indexService;

    @Autowired
    private LdmReadService readService;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private LdmErrorHandlingStorageMappingService errHandlingStorageService;
    
    @Autowired
    private LdmEntityBasicInfoService basicInfoService;

    /**
     * Saves a new version of an LDM entity.
     *
     * @param ldmEntity the LDM entity to save as a new version
     * @param changeRequestId the change request ID
     * @param isRollback whether this is a rollback operation
     * @return the saved LDM entity
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmEntity saveAsNewVersion(@Valid @NotNull LdmEntity ldmEntity, Long changeRequestId, boolean isRollback) {
        LdmEntity existing = readService.getByIdWithAssociationsCurrentVersion(ldmEntity.getId());
        // carry over system-oriented physical materialization mapping to new version
        if (!isRollback) {
            carryOverPhysicalMapping(ldmEntity, existing);
        }
        // get new version
        int currentVersion = existing.getVersion();
        int newVersion = currentVersion + 1;
        // update version in index
        indexService.updateVersion(ldmEntity.getId(), newVersion);
        
        // Update basic info
        basicInfoService.handleBasicInfoUpdate(ldmEntity, existing.getBaseEntityId(), existing.getViewType());
        
        // base entity id should be the same as the initial version
        ldmEntity.setBaseEntityId(existing.getBaseEntityId());
        // create user and time should be the same as the initial version
        ldmEntity.setCreateBy(existing.getCreateBy());
        ldmEntity.setCreateDate(existing.getCreateDate());

        LdmEntity entity = saveVersion(ldmEntity.getId(), newVersion, ldmEntity, changeRequestId);
        // carry over error handling physical storage mapping to new version
        if (!isRollback) {
            carryOverErrorHandlingPhysicalStorageMapping(entity, existing);
        }
        return entity;
    }

    /**
     * Saves a specific version of an LDM entity.
     *
     * @param entityId the entity ID
     * @param newVersion the new version
     * @param ldmEntity the LDM entity to save
     * @param changeRequestId the change request ID
     * @return the saved LDM entity
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmEntity saveVersion(@NotNull Long entityId, @NotNull Integer newVersion, @Valid @NotNull LdmEntity ldmEntity, Long changeRequestId) {
        ldmEntity.setId(entityId);
        ldmEntity.setVersion(newVersion);
        ldmEntity.setRequestId(changeRequestId);
        LdmEntity savedEntity = repository.save(ldmEntity);

        Set<LdmField> fields = ldmEntity.getFields();
        if (fields != null && !fields.isEmpty()) {
            for (LdmField field : fields) {
                field.setId(null);
                field.setRevision(null);
                field.setLdmEntityId(savedEntity.getId());
                field.setLdmVersion(savedEntity.getVersion());
                field.setEntity(savedEntity);
            }
            fieldService.saveAll(savedEntity.getId(), fields);
        }

        return savedEntity;
    }

    /**
     * Carries over error handling physical storage mappings from an existing entity to a new one.
     * This method creates new records in the dec_ldm_error_handling_storage_mapping table
     * with the updated entity ID and version.
     *
     * @param ldmEntity the target LDM entity
     * @param existing the source LDM entity
     */
    private void carryOverErrorHandlingPhysicalStorageMapping(LdmEntity ldmEntity, LdmEntity existing) {
        if (existing.getErrorHandlingStorageMappings() == null || existing.getErrorHandlingStorageMappings().isEmpty()) {
            return;
        }
        
        // Create a new set of mappings with the same physical storage IDs
        Set<LdmErrorHandlingStorageMapping> newMappings = new HashSet<>();
        for (LdmErrorHandlingStorageMapping existingMapping : existing.getErrorHandlingStorageMappings()) {
            LdmErrorHandlingStorageMapping newMapping = new LdmErrorHandlingStorageMapping();
            newMapping.setPhysicalStorageId(existingMapping.getPhysicalStorageId());
            newMappings.add(newMapping);
        }
        
        // Save the new mappings to the database with the new entity ID and version
        errHandlingStorageService.saveErrorHandlingStorageMappings(
                ldmEntity.getId(), 
                ldmEntity.getVersion(),
                newMappings);
        
        // Set the mappings on the entity for in-memory reference
        ldmEntity.setErrorHandlingStorageMappings(newMappings);
    }

    /**
     * Carries over physical mappings from an existing entity to a new one.
     *
     * @param ldmEntity the target LDM entity
     * @param existing the source LDM entity
     */
    private void carryOverPhysicalMapping(LdmEntity ldmEntity, LdmEntity existing) {
        if (ldmEntity.getFields() == null || ldmEntity.getFields().isEmpty()) {
            return;
        }

        Map<String, Set<LdmFieldPhysicalStorageMapping>> fieldPhysicalMapping = fieldService.getSystemFieldPhysicalMapping(existing);
        if (fieldPhysicalMapping.isEmpty()) {
            return;
        }

        ldmEntity.getFields().forEach(field -> {
            if (fieldPhysicalMapping.containsKey(field.getName().toLowerCase(Locale.US))) {
                field.setPhysicalStorageMapping(fieldPhysicalMapping.get(field.getName().toLowerCase(Locale.US)));
            }
        });
    }
}
