package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.DatasetDeployment;
import com.ebay.behavior.gds.mdm.dec.model.DatasetIndex;
import com.ebay.behavior.gds.mdm.dec.model.DatasetPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.dto.DatasetStatusUpdateRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.DatasetStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.MappingSaveMode;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.repository.DatasetDeploymentRepository;
import com.ebay.behavior.gds.mdm.dec.repository.DatasetPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.DatasetRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.toNowSqlTimestamp;

@Slf4j
@Service
@Validated
public class DatasetService extends AbstractVersionModelService<Dataset, DatasetIndex> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<Dataset> modelType = Dataset.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private DatasetRepository repository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private DatasetIndexService indexService;

    @Autowired
    private LdmEntityService ldmEntityService;

    @Autowired
    private DatasetPhysicalStorageMappingRepository mappingRepository;

    @Autowired
    private DatasetDeploymentRepository deploymentRepository;

    @Autowired
    private PhysicalStorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public List<Dataset> getAllCurrentVersion() {
        return repository.findAllCurrentVersion();
    }

    @Override
    @Transactional(readOnly = true)
    public Dataset getByIdCurrentVersion(@NotNull Long id) {
        return repository.findByIdCurrentVersion(id).orElseThrow(() -> new DataNotFoundException(Dataset.class, id));
    }

    @Transactional(readOnly = true)
    public List<Dataset> getAllByNameCurrentVersion(@NotBlank String name) {
        return repository.findAllByNameCurrentVersion(name);
    }

    @Transactional(readOnly = true)
    public List<Dataset> getAllByLdmEntityIdCurrentVersion(@NotNull Long ldmEntityId, @NotNull Integer ldmVersion) {
        return repository.findAllByLdmEntityIdCurrentVersion(ldmEntityId, ldmVersion);
    }

    @Transactional(readOnly = true)
    public List<Dataset> getAllByLdmEntityId(@NotNull Long ldmEntityId) {
        return repository.findAllByLdmEntityId(ldmEntityId);
    }

    @Transactional(readOnly = true)
    public List<Dataset> getAllByNameAndNamespaceCurrentVersion(@NotBlank String name, @NotBlank String namespaceName) {
        return repository.findAllByNameAndNamespaceCurrentVersion(name, namespaceName);
    }

    @Transactional(readOnly = true)
    public List<Dataset> getAllByNamespaceNameCurrentVersion(@NotBlank String namespaceName) {
        return repository.findAllByNamespaceNameCurrentVersion(namespaceName);
    }

    @Transactional(readOnly = true)
    public List<Dataset> searchByNameAndNamespace(String name, String namespaceName) {
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(namespaceName)) {
            return getAllByNameAndNamespaceCurrentVersion(name, namespaceName);
        } else if (StringUtils.isNotBlank(name)) { // To deprecate once usage is removed
            return getAllByNameCurrentVersion(name);
        }
        return getAllByNamespaceNameCurrentVersion(namespaceName);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Dataset create(@Valid @NotNull Dataset dataset) {
        validateName(dataset.getName(), dataset.getNamespaceId());
        validateLdm(dataset.getLdmEntityId(), dataset.getLdmVersion());
        DatasetIndex index = indexService.initialize(dataset.getName());
        dataset.setId(index.getId());
        dataset.setVersion(index.getCurrentVersion());
        return repository.save(dataset);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Dataset updateStatus(@NotNull Long id, @Valid @NotNull String status, DatasetStatusUpdateRequest request) {
        Dataset existing = getByIdCurrentVersion(id);
        if (request != null) {
            if (!Objects.equals(request.id(), id)) {
                throw new IllegalArgumentException("Request ID does not match the dataset ID");
            }
            val env = request.env();
            val newStatus = DatasetStatus.valueOf(request.status().toUpperCase(Locale.US));
            if (env != null) {
                createOrUpdateDeployment(existing, newStatus, env);
            }
            existing.setStatus(newStatus);
            if (StringUtils.isNotBlank(request.updateBy())) {
                existing.setUpdateBy(request.updateBy());
            }
            val updateTime = request.updateDate() != null ? request.updateDate() : toNowSqlTimestamp();
            existing.setUpdateDate(updateTime);
        } else { // to deprecate once usage is removed
            val newStatus = DatasetStatus.valueOf(status.toUpperCase(Locale.US));
            existing.setStatus(newStatus);
            existing.setUpdateDate(toNowSqlTimestamp());
        }
        return repository.save(existing);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createOrUpdateDeployment(@NotNull @Valid Dataset existing, @NotNull DatasetStatus status, @NotNull PlatformEnvironment env) {
        val existingDeployments = deploymentRepository.findByDatasetIdAndDatasetVersionAndEnvironment(existing.getId(), existing.getVersion(), env);
        if (existingDeployments.isEmpty()) {
            DatasetDeployment deployment = DatasetDeployment.builder()
                    .datasetId(existing.getId())
                    .datasetVersion(existing.getVersion())
                    .environment(env)
                    .status(status)
                    .build();
            deploymentRepository.save(deployment);
        } else {
            val existingDeployment = existingDeployments.get(0);
            existingDeployment.setStatus(status);
            deploymentRepository.save(existingDeployment);
        }
    }

    @Transactional(readOnly = true)
    public void validateName(@NotBlank String name, @NotBlank Long namespaceId) {
        List<Dataset> existing = repository.findAllByNameAndNamespaceIdCurrentVersion(name, namespaceId);
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Dataset with name " + name + " already exists in namespace " + namespaceId);
        }
    }

    @Transactional(readOnly = true)
    public void validateLdm(@NotNull Long ldmEntityId, @NotNull Integer ldmVersion) {
        ldmEntityService.getById(VersionedId.of(ldmEntityId, ldmVersion));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void validateModelForUpdate(@Valid @NotNull Dataset model) {
        val existing = getByIdCurrentVersion(model.getId());
        if (existing != null) {
            // validate if there's name conflict
            if (!existing.getName().equalsIgnoreCase(model.getName())) {
                validateName(model.getName(), model.getNamespaceId());
            }

            // validate if the ldm doesn't exist
            validateLdm(model.getLdmEntityId(), model.getLdmVersion());

            // validate if this dataset is not deployed yet.
            if (DatasetStatus.DEPLOYED == model.getStatus()) {
                throw new IllegalArgumentException("Dataset with id %s is deployed, cannot be updated".formatted(model.getId()));
            }
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(@NotNull Long id) {
        getByIdCurrentVersion(id); // ensure dataset exists

        // delete all deployments
        val deployments = deploymentRepository.findByDatasetId(id);
        if (!deployments.isEmpty()) {
            deploymentRepository.deleteAll(deployments);
        }

        // delete all mappings associated with the dataset and its historical versions
        val mappings = mappingRepository.findByDatasetId(id);
        if (!mappings.isEmpty()) {
            mappingRepository.deleteAll(mappings);
        }

        // delete all historical versions
        val historicalVersions = repository.findAllById(id);
        if (!historicalVersions.isEmpty()) {
            repository.deleteAll(historicalVersions);
        }

        // delete index
        indexService.delete(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteByLdmEntityId(@NotNull Long ldmEntityId) {
        // delete datasets
        List<Dataset> datasets = getAllByLdmEntityId(ldmEntityId);
        if (!datasets.isEmpty()) {
            Set<Long> datasetIds = datasets.stream().map(Dataset::getId).collect(Collectors.toSet());
            datasetIds.forEach(this::delete);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<DatasetPhysicalStorageMapping> savePhysicalMappings(@NotNull Long datasetId,
                                                                    @NotNull Set<@NotNull DatasetPhysicalStorageMapping> mappings, MappingSaveMode mode) {
        val dataset = getByIdCurrentVersion(datasetId);
        for (DatasetPhysicalStorageMapping mapping : mappings) {
            mapping.setDatasetId(datasetId);
            if (mapping.getDatasetVersion() == null) {
                mapping.setDatasetVersion(dataset.getVersion());
            }
            storageService.getById(mapping.getPhysicalStorageId()); // Ensure physical storage exists before saving
        }

        val currentMappings = mappingRepository.findByDatasetIdCurrentVersion(datasetId);
        val requestStorageIds = mappings.stream()
                .map(DatasetPhysicalStorageMapping::getPhysicalStorageId)
                .collect(Collectors.toSet());

        if (mode == null || mode == MappingSaveMode.REPLACE_ALL) {
            // delete mapping which not included in request
            val deleteMappings = currentMappings.stream()
                    .filter(mapping -> !requestStorageIds.contains(mapping.getPhysicalStorageId())).toList();
            if (!deleteMappings.isEmpty()) {
                mappingRepository.deleteAll(deleteMappings);
            }
        }

        // create new mappings
        val existingMappingSet = currentMappings.stream()
                .map(DatasetPhysicalStorageMapping::getPhysicalStorageId)
                .filter(requestStorageIds::contains)
                .collect(Collectors.toSet());
        val newMappings = mappings.stream()
                .filter(mapping -> !existingMappingSet.contains(mapping.getPhysicalStorageId()))
                .toList();
        return mappingRepository.saveAll(newMappings);
    }
}