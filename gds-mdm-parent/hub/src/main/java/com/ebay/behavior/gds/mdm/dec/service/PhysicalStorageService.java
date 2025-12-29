package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.DatasetPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.enums.MappingSaveMode;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalStoragePipelineMapping;
import com.ebay.behavior.gds.mdm.dec.repository.DatasetPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalStoragePipelineMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalStorageRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.dec.util.EntityUtils.collectDownstreamLdm;
import static org.hibernate.Hibernate.initialize;

@Slf4j
@Service
@Validated
public class PhysicalStorageService extends AbstractCrudService<PhysicalStorage> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<PhysicalStorage> modelType = PhysicalStorage.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private PhysicalStorageRepository repository;

    @Autowired
    private PhysicalStoragePipelineMappingRepository mappingRepository;

    @Autowired
    private LdmFieldPhysicalStorageMappingRepository fieldMappingRepository;

    @Autowired
    private DatasetPhysicalStorageMappingRepository datasetMappingRepository;

    @Autowired
    private LdmEntityRepository entityRepository;

    @Autowired
    private PipelineService pipelineService;

    @Override
    @Transactional(readOnly = true)
    public Page<PhysicalStorage> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<PhysicalStorage> getAll(String env) {
        if (env != null) {
            val platformEnvironment = PlatformEnvironment.valueOf(env.toUpperCase(Locale.US));
            return repository.findByStorageEnvironment(platformEnvironment);
        }
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PhysicalStorage> getAllByLdmEntityId(@NotNull Long ldmEntityId, Boolean exclusive, String env) {
        Set<Long> storageIds = getLdmStorageIds(ldmEntityId, exclusive);
        return getAllByIdWithAssociations(storageIds, env);
    }

    @Transactional(readOnly = true)
    public List<PhysicalStorage> getAllByLdmEntityIdWithCascade(@NotNull Long ldmEntityId, Boolean exclusive, String env) {
        // get all downstream ldms (including the current one)
        Set<Long> downstreamLdmIdSet = new HashSet<>();
        collectDownstreamLdm(Set.of(ldmEntityId), downstreamLdmIdSet, entityRepository);
        downstreamLdmIdSet.add(ldmEntityId);

        // get all physical storages associated with the downstream ldms
        Set<Long> ldmStorageIds = downstreamLdmIdSet.stream()
                .flatMap(id -> getLdmStorageIds(id, exclusive).stream()).collect(Collectors.toSet());

        return getAllByIdWithAssociations(ldmStorageIds, env);
    }

    @Transactional(readOnly = true)
    public Set<Long> getLdmStorageIds(@NotNull Long ldmEntityId, Boolean exclusive) {
        List<LdmFieldPhysicalStorageMapping> mappings = fieldMappingRepository.findByLdmEntityId(ldmEntityId);
        Set<Long> storageIds = mappings.stream()
                .map(LdmFieldPhysicalStorageMapping::getPhysicalStorageId)
                .collect(Collectors.toSet());
        if (exclusive != null && exclusive) {
            // identify if this storage is also mapped to other datasets
            Set<LdmFieldPhysicalStorageMapping> sharedMappings = fieldMappingRepository.findSharedStorage(storageIds, ldmEntityId);
            Set<Long> sharedStorageIds = sharedMappings.stream()
                    .map(LdmFieldPhysicalStorageMapping::getPhysicalStorageId)
                    .collect(Collectors.toSet());
            // filter out the mappings that are shared with other datasets
            return filterExclusiveStorageIds(storageIds, sharedStorageIds);
        }
        return storageIds;
    }

    private Set<Long> filterExclusiveStorageIds(Set<Long> storageIds, Set<Long> sharedStorageIds) {
        return storageIds.stream()
                .filter(id -> !sharedStorageIds.contains(id))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<PhysicalStorage> getAllByDatasetId(@NotNull Long datasetId, Boolean exclusive, String env) {
        Set<Long> storageIds = getDatasetStorageIds(datasetId, exclusive);
        return getAllByIdWithAssociations(storageIds, env);
    }

    private Set<Long> getDatasetStorageIds(Long datasetId, Boolean exclusive) {
        List<DatasetPhysicalStorageMapping> mappings = datasetMappingRepository.findByDatasetId(datasetId);
        Set<Long> storageIds = mappings.stream()
                .map(DatasetPhysicalStorageMapping::getPhysicalStorageId)
                .collect(Collectors.toSet());
        if (exclusive != null && exclusive) {
            // identify if this storage is also mapped to other datasets
            Set<DatasetPhysicalStorageMapping> sharedMappings = datasetMappingRepository.findSharedStorage(storageIds, datasetId);
            Set<Long> sharedStorageIds = sharedMappings.stream()
                    .map(DatasetPhysicalStorageMapping::getPhysicalStorageId)
                    .collect(Collectors.toSet());
            // filter out the mappings that are shared with other datasets
            return filterExclusiveStorageIds(storageIds, sharedStorageIds);
        }
        return storageIds;
    }

    @Override
    @Transactional(readOnly = true)
    public PhysicalStorage getByIdWithAssociations(long id) {
        val storage = repository.findById(id).orElseThrow(() -> new DataNotFoundException(PhysicalStorage.class, id));
        initialize(storage.getPipelines());
        return storage;
    }

    @Transactional(readOnly = true)
    public List<PhysicalStorage> getAllByIdWithAssociations(Set<Long> storageIds) {
        List<PhysicalStorage> storages = repository.findAllById(storageIds);
        storages.forEach(s -> initialize(s.getPipelines()));
        return storages;
    }

    @Transactional(readOnly = true)
    public List<PhysicalStorage> getAllByIdWithAssociations(Set<Long> storageIds, String env) {
        List<PhysicalStorage> storages = getAllByIdWithAssociations(storageIds);
        if (storages != null && env != null) {
            val platformEnvironment = PlatformEnvironment.valueOf(env.toUpperCase(Locale.US));
            return storages.stream()
                    .filter(s -> s.getStorageEnvironment() == platformEnvironment)
                    .collect(Collectors.toList());
        }
        return storages;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id); // ensure storage exists
        // delete all field mappings associated with the storage
        val fieldMappings = fieldMappingRepository.findByPhysicalStorageId(id);
        if (!fieldMappings.isEmpty()) {
            fieldMappingRepository.deleteAll(fieldMappings);
        }
        // delete all dataset mappings associated with the storage
        val datasetMappings = datasetMappingRepository.findByPhysicalStorageId(id);
        if (!datasetMappings.isEmpty()) {
            datasetMappingRepository.deleteAll(datasetMappings);
        }
        super.delete(id);
    }

    @Override
    public PhysicalStorage create(PhysicalStorage model) {
        List<PhysicalStorage> existingStorages = repository.findByAccessTypeAndStorageDetailsAndStorageEnvironment(
                model.getAccessType(), model.getStorageDetails(), model.getStorageEnvironment());
        if (!existingStorages.isEmpty()) {
            log.info("Storage with access type {} and storage details {} already exists in storage env {}. Returning existing storage.",
                    model.getAccessType(), model.getStorageDetails(), model.getStorageEnvironment());
            return existingStorages.get(0);
        }
        return super.create(model);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalStorage savePipelineMappings(@NotNull Long storageId, @NotNull Set<@Valid @NotNull Pipeline> pipelines, MappingSaveMode mode) {
        if (pipelines.isEmpty()) {
            throw new IllegalArgumentException("Pipelines cannot be empty");
        }

        // create / update pipelines to generate ids
        val savedPipelines = pipelineService.saveAll(pipelines);

        // get all dls pipeline ids
        val dlsPipelineIds = savedPipelines.stream().map(Pipeline::getPipelineId).collect(Collectors.toSet());

        // get all existing mapped pipelines for the storage
        val existingMappings = mappingRepository.findByPhysicalStorageId(storageId);
        val existingPipelineIds = existingMappings.stream()
                .map(mapping -> mapping.getPipeline().getPipelineId())
                .collect(Collectors.toSet());

        if (mode == null || mode == MappingSaveMode.REPLACE_ALL) {
            // collect the mappings which should be deleted as they are not in the new set
            val deletedMappings = existingMappings.stream()
                    .filter(mapping -> !dlsPipelineIds.contains(mapping.getPipeline().getPipelineId()))
                    .map(PhysicalStoragePipelineMapping::getId)
                    .collect(Collectors.toSet());
            deletedMappings.forEach(mappingId -> mappingRepository.deleteById(mappingId));
        }

        val newMappings = savedPipelines.stream()
                .filter(pipeline -> !existingPipelineIds.contains(pipeline.getPipelineId()))
                .collect(Collectors.toSet());
        newMappings.forEach(pipeline -> createMapping(storageId, pipeline.getId()));

        return getByIdWithAssociations(storageId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createMapping(long storageId, long pipelineId) {
        val existingMapping = mappingRepository.findByPhysicalStorageIdAndPipelineId(storageId, pipelineId);
        if (existingMapping.isPresent()) {
            throw new IllegalArgumentException("Mapping already exists for storageId=" + storageId + " and pipelineId=" + pipelineId);
        }
        val storage = getById(storageId);
        val pipeline = pipelineService.getById(pipelineId);
        val mapping = new PhysicalStoragePipelineMapping(storage, pipeline);
        mappingRepository.save(mapping);
    }

    @Transactional(readOnly = true)
    public List<PhysicalStorage> getAllByPhysicalAssetId(@NotNull Long assetId) {
        List<PhysicalStorage> storages = repository.findByPhysicalAssetId(assetId);
        Set<Long> storageIds = storages.stream().map(PhysicalStorage::getId).collect(Collectors.toSet());
        return getAllByIdWithAssociations(storageIds);
    }
}
