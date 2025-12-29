package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.DatasetPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.StorageContext;
import com.ebay.behavior.gds.mdm.dec.repository.DatasetPhysicalStorageMappingRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Validated
@Service
public class DatasetPhysicalStorageMappingService extends AbstractCrudService<DatasetPhysicalStorageMapping> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<DatasetPhysicalStorageMapping> modelType = DatasetPhysicalStorageMapping.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private DatasetPhysicalStorageMappingRepository repository;

    @Autowired
    private PhysicalStorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public Page<DatasetPhysicalStorageMapping> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    public DatasetPhysicalStorageMapping getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<DatasetPhysicalStorageMapping> getAllByDatasetIdCurrentVersion(@NotNull Long datasetId) {
        return repository.findByDatasetIdCurrentVersion(datasetId);
    }

    @Transactional(readOnly = true)
    public List<DatasetPhysicalStorageMapping> getAllByDatasetId(@NotNull Long datasetId, Boolean latest) {
        val currentMappings = getAllByDatasetIdCurrentVersion(datasetId);
        if (latest != null && latest) { // return the latest available mappings for a dataset
            // if there are current mappings, return them
            if (currentMappings != null && !currentMappings.isEmpty()) {
                return currentMappings;
            }

            // if there are no current mappings, return all the mappings for a dataset
            val allMappings = repository.findByDatasetId(datasetId);
            if (allMappings == null || allMappings.isEmpty()) {
                return List.of();
            }

            // get the latest dataset version
            val latestDeployedVersion = allMappings.stream()
                    .map(DatasetPhysicalStorageMapping::getDatasetVersion).max(Comparator.naturalOrder()).get();
            return allMappings.stream().filter(m -> Objects.equals(m.getDatasetVersion(), latestDeployedVersion)).toList();
        }
        return currentMappings;
    }

    @Transactional(readOnly = true)
    public List<DatasetPhysicalStorageMapping> getAllByDatasetIdAndEnvironment(@NotNull Long datasetId, String env, Boolean latest) {
        val deployments = getAllByDatasetId(datasetId, latest);
        if (deployments != null && env != null) {
            val platformEnvironment = PlatformEnvironment.valueOf(env.toUpperCase(Locale.US));
            return deployments.stream().filter(d -> {
                val storageId = d.getPhysicalStorageId();
                val storage = storageService.getById(storageId);
                return storage.getStorageEnvironment().equals(platformEnvironment);
            }).toList();
        }
        return deployments;
    }

    @Transactional(readOnly = true)
    public List<DatasetPhysicalStorageMapping> getAllByDatasetIdAndEnvironmentAndStorageContexts(
            @NotNull Long datasetId, 
            String env, 
            List<StorageContext> storageContexts, 
            Boolean latest) {
        val deployments = getAllByDatasetIdAndEnvironment(datasetId, env, latest);
        if (deployments != null && !deployments.isEmpty() && storageContexts != null && !storageContexts.isEmpty()) {
            return deployments.stream().filter(d -> {
                val storageId = d.getPhysicalStorageId();
                val storage = storageService.getById(storageId);
                return storage.getStorageContext() != null && storageContexts.contains(storage.getStorageContext());
            }).toList();
        }
        return deployments;
    }

    @Transactional(readOnly = true)
    public List<DatasetPhysicalStorageMapping> getAllByDatasetIdAndEnvironmentAndStorageContexts(
            @NotNull Long datasetId,
            String env,
            String systemContext,
            Boolean latest) {
        // Split the systemContext string by commas and convert each part to a StorageContext enum
        List<StorageContext> storageContexts = new ArrayList<>();
        String[] contextParts = systemContext.split(",");

        for (String contextPart : contextParts) {
            String trimmedPart = contextPart.trim();
            if (StringUtils.isNotEmpty(trimmedPart)) {
                StorageContext storageContextEnum = StorageContext.valueOf(trimmedPart.toUpperCase(Locale.US));
                storageContexts.add(storageContextEnum);
            }
        }

        return getAllByDatasetIdAndEnvironmentAndStorageContexts(datasetId, env, storageContexts, latest);
    }
}
