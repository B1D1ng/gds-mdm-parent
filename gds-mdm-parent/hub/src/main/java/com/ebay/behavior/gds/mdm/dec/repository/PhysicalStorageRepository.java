package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhysicalStorageRepository extends JpaRepository<PhysicalStorage, Long> {

    List<PhysicalStorage> findByAccessTypeAndStorageDetailsAndStorageEnvironment(AccessType accessType, String storageDetails,
                                                                                 PlatformEnvironment storageEnvironment);

    List<PhysicalStorage> findByPhysicalAssetId(Long physicalAssetId);

    List<PhysicalStorage> findByStorageEnvironment(PlatformEnvironment storageEnvironment);
}
