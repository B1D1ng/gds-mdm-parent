package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldSignalMapping;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmFieldPhysicalMappingRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.StorageContext;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldSignalMappingRepository;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.toNowSqlTimestamp;
import static org.hibernate.Hibernate.initialize;

@Service
@Validated
public class LdmFieldService extends AbstractCrudService<LdmField> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<LdmField> modelType = LdmField.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmFieldRepository repository;

    @Autowired
    private LdmFieldSignalMappingRepository signalMappingRepository;

    @Autowired
    private LdmFieldPhysicalStorageMappingRepository physicalMappingRepository;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<LdmField> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<LdmField> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public LdmField getByIdWithAssociations(long id) {
        val field = repository.findById(id).orElseThrow(() -> new DataNotFoundException(LdmEntity.class, String.valueOf(id)));
        initialize(field.getSignalMapping());
        initialize(field.getPhysicalStorageMapping());
        return field;
    }

    @Transactional(readOnly = true)
    public List<LdmField> getAllByEntityId(@NotNull Long entityId) {
        return repository.findByEntityId(entityId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void validateName(@NotBlank String fieldName, @NotNull List<@Valid @NotNull LdmField> existingFields) {
        if (!existingFields.isEmpty()) {
            for (LdmField field : existingFields) {
                if (field.getName().equalsIgnoreCase(fieldName)) {
                    throw new IllegalArgumentException("Field name %s already exists in entity %s and version %s"
                            .formatted(fieldName, field.getLdmEntityId(), field.getLdmVersion()));
                }
            }
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Set<LdmField> updateFields(@NotNull Long ldmEntityId, @NotNull @Valid LdmEntity entity, @NotNull Set<LdmField> fields) {
        List<LdmField> existingFields = repository.findByEntityId(ldmEntityId);
        fields.forEach(field -> {
            field.setLdmEntityId(ldmEntityId);
            field.setLdmVersion(entity.getVersion());

            // for new field, validate if the field name already exists
            if (field.getId() == null && field.getName() != null) {
                validateName(field.getName(), existingFields);
            }
            repository.saveAndFlush(field);
        });
        return fields;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<LdmFieldPhysicalStorageMapping> updateFieldPhysicalMappings(
            @NotNull Long ldmEntityId, @NotNull Set<LdmFieldPhysicalMappingRequest> mappingRequests) {
        Set<Long> updateIds = new HashSet<>();
        for (LdmFieldPhysicalMappingRequest request : mappingRequests) {
            Long fieldId = request.fieldId();

            // validate if the field exists
            LdmField field = getById(fieldId);
            if (!Objects.equals(field.getLdmEntityId(), ldmEntityId)) {
                throw new IllegalArgumentException("Field %s does not belong to entity %s".formatted(fieldId, ldmEntityId));
            }

            Set<Long> storageIds = request.storageIds();

            // get current mappings
            List<LdmFieldPhysicalStorageMapping> existingMappings = physicalMappingRepository.findByLdmFieldId(fieldId);
            Set<Long> existingMappingStorageIdSet = existingMappings.stream()
                    .map(LdmFieldPhysicalStorageMapping::getPhysicalStorageId)
                    .collect(Collectors.toSet());

            // collect the mappings which should be deleted as they are not in the new set
            Set<Long> mappingsToDelete = existingMappings.stream()
                    .filter(mapping -> !storageIds.contains(mapping.getPhysicalStorageId()))
                    .map(LdmFieldPhysicalStorageMapping::getId)
                    .collect(Collectors.toSet());
            Set<Long> mappingsToKeep = existingMappings.stream()
                    .filter(mapping -> storageIds.contains(mapping.getPhysicalStorageId()))
                    .map(LdmFieldPhysicalStorageMapping::getId)
                    .collect(Collectors.toSet());
            updateIds.addAll(mappingsToKeep);

            // collect new mappings
            Set<Long> mappingsToCreate = storageIds.stream()
                    .filter(storageId -> !existingMappingStorageIdSet.contains(storageId))
                    .collect(Collectors.toSet());

            mappingsToDelete.forEach(mappingId -> physicalMappingRepository.deleteById(mappingId));

            mappingsToCreate.forEach(storageId -> {
                val createDate = request.createDate() == null ? toNowSqlTimestamp() : request.createDate();
                val created = createMapping(storageId, fieldId, request.createBy(), createDate);
                updateIds.add(created.getId());
            });
        }

        return physicalMappingRepository.findAllById(updateIds);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<LdmFieldPhysicalStorageMapping> updateFieldPhysicalStorageMappings(
            @NotNull Long ldmEntityId, @NotNull Set<LdmFieldPhysicalStorageMapping> mappingRequests) {
        // Validate storage ids
        Set<Long> allStorageIds = mappingRequests.stream()
                .map(LdmFieldPhysicalStorageMapping::getPhysicalStorageId)
                .collect(Collectors.toSet());
        allStorageIds.forEach(storageService::getById);

        // Validate field ids
        Set<Long> allFieldIds = mappingRequests.stream().map(LdmFieldPhysicalStorageMapping::getLdmFieldId).collect(Collectors.toSet());
        for (Long fieldId : allFieldIds) {
            LdmField field = getById(fieldId);
            if (!Objects.equals(field.getLdmEntityId(), ldmEntityId)) {
                throw new IllegalArgumentException("Field %s does not belong to entity %s".formatted(fieldId, ldmEntityId));
            }
        }

        // convert mappingRequests to map: fieldid -> map <storage_id, mapping>
        Map<Long, Map<Long, LdmFieldPhysicalStorageMapping>> mappingGroups = mappingRequests.stream()
            .collect(Collectors.groupingBy(
                LdmFieldPhysicalStorageMapping::getLdmFieldId,
                Collectors.toMap(
                    LdmFieldPhysicalStorageMapping::getPhysicalStorageId,
                    m -> m,
                    (m1, m2) -> m1 // deduplicate field -> storage id mapping
                )
            ));

        Set<Long> updateIds = new HashSet<>();
        for (Map.Entry<Long, Map<Long, LdmFieldPhysicalStorageMapping>> fieldMappingGroup : mappingGroups.entrySet()) {
            // get field id, mapped storage ids
            Long fieldId = fieldMappingGroup.getKey();
            Map<Long, LdmFieldPhysicalStorageMapping> fieldMappingRequests = fieldMappingGroup.getValue();
            Set<Long> requestStorageIds = fieldMappingRequests.keySet();

            // get current field mappings from db
            List<LdmFieldPhysicalStorageMapping> currentMappings = physicalMappingRepository.findByLdmFieldId(fieldId);

            // Delete mappings not included in request
            List<LdmFieldPhysicalStorageMapping> mappingsToDelete = currentMappings.stream()
                .filter(m -> !requestStorageIds.contains(m.getPhysicalStorageId())).toList();

            // Update existing mappings in request
            List<LdmFieldPhysicalStorageMapping> mappingsToKeep = currentMappings.stream()
                .filter(m -> requestStorageIds.contains(m.getPhysicalStorageId())).toList();
            for (LdmFieldPhysicalStorageMapping mapping : mappingsToKeep) {
                LdmFieldPhysicalStorageMapping mappingRequest = fieldMappingRequests.get(mapping.getPhysicalStorageId());
                mapping.setPhysicalFieldExpression(mappingRequest.getPhysicalFieldExpression());
            }

            // Create new mappings
            Set<Long> existingStorageIds = currentMappings.stream().map(LdmFieldPhysicalStorageMapping::getPhysicalStorageId).collect(Collectors.toSet());
            List<LdmFieldPhysicalStorageMapping> mappingsToCreate = fieldMappingRequests.entrySet().stream()
                .filter(e -> !existingStorageIds.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .peek(m -> {
                    m.setId(null);
                    m.setRevision(null);
                })
                .toList();

            // updates in db
            if (!mappingsToDelete.isEmpty()) {
                physicalMappingRepository.deleteAll(mappingsToDelete);
            }
            if (!mappingsToKeep.isEmpty()) {
                physicalMappingRepository.saveAll(mappingsToKeep);
                updateIds.addAll(mappingsToKeep.stream().map(LdmFieldPhysicalStorageMapping::getId).collect(Collectors.toSet()));
            }
            if (!mappingsToCreate.isEmpty()) {
                List<LdmFieldPhysicalStorageMapping> created = physicalMappingRepository.saveAll(mappingsToCreate);
                updateIds.addAll(created.stream().map(LdmFieldPhysicalStorageMapping::getId).collect(Collectors.toSet()));
            }
        }
        return physicalMappingRepository.findAllById(updateIds);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmFieldPhysicalStorageMapping createMapping(long storageId, long fieldId, String createBy, Timestamp createDate) {
        storageService.getById(storageId); // ensure storage exists
        val createTimestamp = createDate == null ? toNowSqlTimestamp() : createDate;
        val mapping = LdmFieldPhysicalStorageMapping.builder()
                .physicalStorageId(storageId)
                .ldmFieldId(fieldId)
                .createBy(createBy)
                .createDate(createTimestamp)
                .updateBy(createBy)
                .updateDate(createTimestamp)
                .build();
        return physicalMappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<LdmField> saveAll(@NotNull Long ldmEntityId, @NotNull Set<@NotNull @Valid LdmField> fields) {
        List<LdmField> existingFields = repository.findByEntityId(ldmEntityId);
        for (LdmField field : fields) {
            if (field.getId() == null && existingFields != null) {
                validateName(field.getName(), existingFields);
            }
        }

        List<LdmField> savedFields = repository.saveAllAndFlush(fields);

        // Save new mappings
        Set<LdmFieldSignalMapping> signalMappings = new HashSet<>();
        Set<LdmFieldPhysicalStorageMapping> storageMappings = new HashSet<>();
        for (LdmField field : savedFields) {
            if (field.getSignalMapping() != null) {
                Set<LdmFieldSignalMapping> signalMapping = field.getSignalMapping();
                signalMappings.addAll(signalMapping.stream().peek(m -> {
                    entityManager.detach(m);
                    m.setLdmFieldId(field.getId());
                    m.setId(null);
                }).toList());
            }
            if (field.getPhysicalStorageMapping() != null) {
                Set<LdmFieldPhysicalStorageMapping> storageMapping = field.getPhysicalStorageMapping();
                storageMappings.addAll(storageMapping.stream().peek(m -> {
                    entityManager.detach(m);
                    m.setLdmFieldId(field.getId());
                    m.setId(null);
                }).toList());
            }
        }
        signalMappingRepository.saveAll(signalMappings);
        physicalMappingRepository.saveAll(storageMappings);

        return savedFields;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id); // ensure field exists
        // delete all mappings associated with the field
        val signalMappings = signalMappingRepository.findByLdmFieldId(id);
        if (!signalMappings.isEmpty()) {
            signalMappingRepository.deleteAll(signalMappings);
        }
        val physicalMappings = physicalMappingRepository.findByLdmFieldId(id);
        if (!physicalMappings.isEmpty()) {
            physicalMappingRepository.deleteAll(physicalMappings);
        }
        super.delete(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteByLdmEntityId(@NotNull Long ldmEntityId) {
        val fields = repository.findByEntityIdAllVersions(ldmEntityId);
        val fieldIds = fields.stream().map(LdmField::getId).toList();
        if (!fieldIds.isEmpty()) {
            fieldIds.forEach(this::delete);
        }
    }

    /**
     * Gets the system field physical mappings for an entity.
     * This method filters physical storage mappings to only include those with SYSTEM storage context.
     *
     * @param entity The LdmEntity to get system field physical mappings for
     * @return A map of field names to their system physical storage mappings
     */
    @Transactional(readOnly = true)
    public Map<String, Set<LdmFieldPhysicalStorageMapping>> getSystemFieldPhysicalMapping(@NotNull LdmEntity entity) {
        Map<String, Set<LdmFieldPhysicalStorageMapping>> fieldPhysicalMapping = new HashMap<>();
        
        // If no fields, return empty map
        Set<LdmField> fields = entity.getFields();
        if (fields == null || fields.isEmpty()) {
            return fieldPhysicalMapping;
        }
        
        // Process each field
        fields.stream()
            .filter(field -> hasPhysicalMappings(field))
            .forEach(field -> processFieldMappings(field, fieldPhysicalMapping));
            
        return fieldPhysicalMapping;
    }
    
    /**
     * Checks if a field has physical mappings.
     *
     * @param field The field to check
     * @return true if the field has physical mappings, false otherwise
     */
    private boolean hasPhysicalMappings(LdmField field) {
        Set<LdmFieldPhysicalStorageMapping> mappings = field.getPhysicalStorageMapping();
        return mappings != null && !mappings.isEmpty();
    }
    
    /**
     * Processes a field's physical mappings, filtering for SYSTEM context and adding to the result map.
     *
     * @param field The field to process
     * @param resultMap The map to add results to
     */
    private void processFieldMappings(LdmField field, Map<String, Set<LdmFieldPhysicalStorageMapping>> resultMap) {
        Set<LdmFieldPhysicalStorageMapping> mappings = field.getPhysicalStorageMapping();
        
        // Filter for system storage context
        Set<LdmFieldPhysicalStorageMapping> systemMappings = mappings.stream()
            .filter(this::isSystemStorageMapping)
            .collect(Collectors.toSet());
            
        // Add to result map if we found system mappings
        if (!systemMappings.isEmpty()) {
            resultMap.put(field.getName().toLowerCase(Locale.US), systemMappings);
        }
    }
    
    /**
     * Checks if a mapping is for a system storage context.
     *
     * @param mapping The mapping to check
     * @return true if the mapping is for a system storage, false otherwise
     */
    private boolean isSystemStorageMapping(LdmFieldPhysicalStorageMapping mapping) {
        PhysicalStorage storage = storageService.getById(mapping.getPhysicalStorageId());
        return storage.getStorageContext() == StorageContext.SYSTEM;
    }
}
