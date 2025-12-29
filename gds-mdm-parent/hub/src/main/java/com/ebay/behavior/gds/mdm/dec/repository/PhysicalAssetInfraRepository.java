package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfra;
import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.PropertyType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for PhysicalAssetInfra entity.
 */
@Repository
public interface PhysicalAssetInfraRepository extends JpaRepository<PhysicalAssetInfra, Long> {

    List<PhysicalAssetInfra> findByInfraType(InfraType infraType);

    List<PhysicalAssetInfra> findByPropertyType(PropertyType propertyType);

    List<PhysicalAssetInfra> findByPlatformEnvironment(PlatformEnvironment platformEnvironment);

    List<PhysicalAssetInfra> findByInfraTypeAndPropertyType(InfraType infraType, PropertyType propertyType);

    List<PhysicalAssetInfra> findByInfraTypeAndPropertyTypeAndPlatformEnvironment(
            InfraType infraType, PropertyType propertyType, PlatformEnvironment platformEnvironment);
}