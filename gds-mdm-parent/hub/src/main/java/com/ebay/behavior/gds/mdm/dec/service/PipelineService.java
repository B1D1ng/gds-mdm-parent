package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalStoragePipelineMapping;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalStoragePipelineMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PipelineRepository;

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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
public class PipelineService extends AbstractCrudService<Pipeline> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<Pipeline> modelType = Pipeline.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private PipelineRepository repository;

    @Autowired
    private PhysicalStoragePipelineMappingRepository mappingRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Pipeline> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<Pipeline> getAll() {
        return repository.findAll();
    }

    @Override
    public Pipeline getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<Pipeline> getByPipelineId(@NotNull String pipelineId) {
        return repository.findByPipelineId(pipelineId);
    }

    @Transactional(readOnly = true)
    public List<Pipeline> getByStorageId(@NotNull Long storageId, Boolean exclusive) {
        List<PhysicalStoragePipelineMapping> mappings = mappingRepository.findByPhysicalStorageId(storageId);
        List<Pipeline> pipelines = mappings.stream().map(PhysicalStoragePipelineMapping::getPipeline).toList();

        if (pipelines.isEmpty() || exclusive == null || !exclusive) {
            return pipelines;
        }

        Set<Long> pipelineIds = pipelines.stream().map(Pipeline::getId).collect(Collectors.toSet());
        List<PhysicalStoragePipelineMapping> sharedMappings = mappingRepository.getSharedPipelines(pipelineIds, storageId);
        Set<Long> sharedPipelineIds = sharedMappings.stream().map(x -> x.getPipeline().getId()).collect(Collectors.toSet());

        return pipelines.stream().filter(p -> !sharedPipelineIds.contains(p.getId())).toList();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Pipeline create(@Valid @NotNull Pipeline pipeline) {
        var sameIdPipelines = repository.findByPipelineId(pipeline.getPipelineId());
        if (sameIdPipelines != null && !sameIdPipelines.isEmpty()) {
            throw new IllegalArgumentException("Pipeline with the same pipelineId %s already exists".formatted(pipeline.getPipelineId()));
        }
        return super.create(pipeline);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Pipeline update(@Valid @NotNull Pipeline pipeline) {
        val existingPipeline = getById(pipeline.getId());
        if (!Objects.equals(existingPipeline.getPipelineId(), pipeline.getPipelineId())) {
            throw new IllegalArgumentException("Cannot update pipelineId");
        }
        return super.update(pipeline);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        val mappings = mappingRepository.findByPipelineId(id);
        if (!mappings.isEmpty()) {
            mappingRepository.deleteAll(mappings);
        }
        super.delete(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Set<Pipeline> saveAll(@NotNull Set<@Valid @NotNull Pipeline> pipelines) {
        return pipelines.stream().map(pipeline -> {
            if (Objects.isNull(pipeline.getId()) || Objects.isNull(pipeline.getRevision())) {
                List<Pipeline> existingDlsPipelines = getByPipelineId(pipeline.getPipelineId());
                if (!existingDlsPipelines.isEmpty()) {
                    log.info("Pipeline with pipelineId {} already exists. Returning existing pipeline.", pipeline.getPipelineId());
                    pipeline.setId(existingDlsPipelines.get(0).getId());
                } else {
                    log.info("Creating new pipeline with pipelineId {}", pipeline.getPipelineId());
                    pipeline = create(pipeline);
                }
            }
            return pipeline;
        }).collect(Collectors.toSet());
    }
}
