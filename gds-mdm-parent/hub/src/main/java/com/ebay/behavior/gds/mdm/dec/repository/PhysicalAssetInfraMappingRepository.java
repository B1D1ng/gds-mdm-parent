package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfra;
import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalAssetInfraMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PhysicalAssetInfraMapping entity.
 */
@Repository
public interface PhysicalAssetInfraMappingRepository extends JpaRepository<PhysicalAssetInfraMapping, Long> {

    /**
     * Find all mappings for a specific physical asset.
     *
     * @param physicalAsset the physical asset
     * @return list of mappings
     */
    List<PhysicalAssetInfraMapping> findByPhysicalAsset(PhysicalAsset physicalAsset);

    /**
     * Find all mappings for a specific physical asset id.
     */
    List<PhysicalAssetInfraMapping> findByPhysicalAssetId(Long physicalAssetId);

    /**
     * Find all mappings for a specific physical asset infrastructure.
     *
     * @param physicalAssetInfra the physical asset infrastructure
     * @return list of mappings
     */
    List<PhysicalAssetInfraMapping> findByPhysicalAssetInfra(PhysicalAssetInfra physicalAssetInfra);

    /**
     * Find a mapping by physical asset and physical asset infrastructure.
     *
     * @param physicalAsset the physical asset
     * @param physicalAssetInfra the physical asset infrastructure
     * @return optional mapping
     */
    Optional<PhysicalAssetInfraMapping> findByPhysicalAssetAndPhysicalAssetInfra(
            PhysicalAsset physicalAsset, PhysicalAssetInfra physicalAssetInfra);
}