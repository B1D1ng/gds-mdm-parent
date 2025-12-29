package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.enums.PhysicalAssetType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhysicalAssetRepository extends JpaRepository<PhysicalAsset, Long> {
    List<PhysicalAsset> findAllByAssetTypeAndAssetName(PhysicalAssetType assetType, String assetName);
}
