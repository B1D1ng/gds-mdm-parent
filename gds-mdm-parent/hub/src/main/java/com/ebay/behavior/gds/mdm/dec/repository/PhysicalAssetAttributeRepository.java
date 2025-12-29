package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetAttribute;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhysicalAssetAttributeRepository extends JpaRepository<PhysicalAssetAttribute, Long> {

    /**
     * Find all attribute records associated with the given asset ID
     *
     * @param assetId ID of the physical asset
     * @return List of PhysicalAssetAttribute records
     */
    List<PhysicalAssetAttribute> findByAssetId(Long assetId);
}
