package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalStoragePipelineMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PhysicalStoragePipelineMappingRepository
        extends JpaRepository<PhysicalStoragePipelineMapping, Long> {

    Optional<PhysicalStoragePipelineMapping> findByPhysicalStorageIdAndPipelineId(Long physicalStorageId, Long pipelineId);

    List<PhysicalStoragePipelineMapping> findByPhysicalStorageId(Long physicalStorageId);

    List<PhysicalStoragePipelineMapping> findByPipelineId(Long pipelineId);

    @Query("SELECT mp FROM PhysicalStoragePipelineMapping mp WHERE mp.physicalStorage.id IN :physicalStorageIds")
    Set<PhysicalStoragePipelineMapping> getPipelines(Set<Long> physicalStorageIds);

    @Query("SELECT mp FROM PhysicalStoragePipelineMapping mp WHERE mp.pipeline.id IN :pipelineIds AND mp.physicalStorage.id <> :storageId")
    List<PhysicalStoragePipelineMapping> getSharedPipelines(Set<Long> pipelineIds, Long storageId);
}
