package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.LdmErrorHandlingStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.repository.LdmErrorHandlingStorageMappingRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing LDM error handling physical storage mappings.
 */
@Validated
@Service
public class LdmErrorHandlingStorageMappingService extends AbstractCrudService<LdmErrorHandlingStorageMapping> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<LdmErrorHandlingStorageMapping> modelType = LdmErrorHandlingStorageMapping.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmErrorHandlingStorageMappingRepository repository;

    @Autowired
    private PhysicalStorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public Page<LdmErrorHandlingStorageMapping> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    public LdmErrorHandlingStorageMapping getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }

    /**
     * Get all mappings for an LDM entity with current version.
     *
     * @param ldmEntityId the LDM entity ID
     * @return list of mappings
     */
    @Transactional(readOnly = true)
    public List<LdmErrorHandlingStorageMapping> getAllByLdmEntityIdCurrentVersion(@NotNull Long ldmEntityId) {
        return repository.findByLdmEntityIdCurrentVersion(ldmEntityId);
    }

    /**
     * Get all mappings for an LDM entity and environment
     *
     * @param ldmEntityId the LDM entity ID
     * @param env the environment
     * @return list of mappings
     */
    @Transactional(readOnly = true)
    public List<LdmErrorHandlingStorageMapping> getAllByLdmEntityIdAndEnvironment(@NotNull Long ldmEntityId, String env, Boolean latest) {
        val mappings = getAllByLdmEntityIdCurrentVersion(ldmEntityId);
        if (mappings != null && env != null) {
            val platformEnvironment = PlatformEnvironment.valueOf(env.toUpperCase(Locale.US));
            return mappings.stream().filter(m -> {
                val storageId = m.getPhysicalStorageId();
                val storage = storageService.getById(storageId);
                return storage.getStorageEnvironment().equals(platformEnvironment);
            }).toList();
        }
        return mappings;
    }

    /**
     * Get all mappings for an LDM entity and version.
     *
     * @param ldmEntityId the LDM entity ID
     * @param ldmVersion the LDM version
     * @return list of mappings
     */
    @Transactional(readOnly = true)
    public List<LdmErrorHandlingStorageMapping> getAllByLdmEntityIdAndVersion(@NotNull Long ldmEntityId, @NotNull Integer ldmVersion) {
        return repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
    }
    
    /**
     * Get all mappings for an LDM entity, version, and environment.
     *
     * @param ldmEntityId the LDM entity ID
     * @param ldmVersion the LDM version
     * @param env the environment
     * @return list of mappings
     */
    @Transactional(readOnly = true)
    public List<LdmErrorHandlingStorageMapping> getAllByLdmEntityIdAndVersionAndEnvironment(
            @NotNull Long ldmEntityId, @NotNull Integer ldmVersion, String env) {
        val mappings = getAllByLdmEntityIdAndVersion(ldmEntityId, ldmVersion);
        if (mappings != null && env != null) {
            val platformEnvironment = PlatformEnvironment.valueOf(env.toUpperCase(Locale.US));
            return mappings.stream().filter(m -> {
                val storageId = m.getPhysicalStorageId();
                val storage = storageService.getById(storageId);
                return storage.getStorageEnvironment().equals(platformEnvironment);
            }).toList();
        }
        return mappings;
    }
    
    /**
     * Save error handling physical mappings for an LDM entity.
     *
     * @param ldmEntityId the LDM entity ID
     * @param ldmVersion the LDM entity version
     * @param mappings the set of mappings to save
     * @return the saved mappings
     */
    @Transactional
    public List<LdmErrorHandlingStorageMapping> saveErrorHandlingStorageMappings(
            @NotNull Long ldmEntityId, @NotNull Integer ldmVersion, @NotNull Set<@NotNull LdmErrorHandlingStorageMapping> mappings) {
        // Set the LDM entity ID and version for each mapping
        for (LdmErrorHandlingStorageMapping mapping : mappings) {
            mapping.setLdmEntityId(ldmEntityId);
            mapping.setLdmVersion(ldmVersion);
            
            // Ensure physical storage exists before saving
            storageService.getById(mapping.getPhysicalStorageId());
        }
        
        // Delete mappings not included in the request
        List<LdmErrorHandlingStorageMapping> currentMappings = repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
        Set<Long> requestStorageIds = mappings.stream()
                .map(LdmErrorHandlingStorageMapping::getPhysicalStorageId)
                .collect(Collectors.toSet());
        
        List<LdmErrorHandlingStorageMapping> mappingsToDelete = currentMappings.stream()
                .filter(mapping -> !requestStorageIds.contains(mapping.getPhysicalStorageId()))
                .toList();
        
        if (!mappingsToDelete.isEmpty()) {
            repository.deleteAll(mappingsToDelete);
        }
        
        // Create new mappings
        Set<Long> existingMappingStorageIds = currentMappings.stream()
                .map(LdmErrorHandlingStorageMapping::getPhysicalStorageId)
                .filter(requestStorageIds::contains)
                .collect(Collectors.toSet());
        
        List<LdmErrorHandlingStorageMapping> newMappings = mappings.stream()
                .filter(mapping -> !existingMappingStorageIds.contains(mapping.getPhysicalStorageId()))
                .toList();
        
        return repository.saveAll(newMappings);
    }
}
