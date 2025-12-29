package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalAssetLdmMapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhysicalAssetLdmMappingRepository
        extends JpaRepository<PhysicalAssetLdmMapping, Long> {

    List<PhysicalAssetLdmMapping> findByPhysicalAssetId(Long physicalAssetId);

    List<PhysicalAssetLdmMapping> findByLdmBaseEntityId(Long ldmBaseEntityId);

    Optional<PhysicalAssetLdmMapping> findByPhysicalAssetIdAndLdmBaseEntityId(Long physicalAssetId, Long ldmBaseEntityId);
}
