package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfraGlobalProperty;
import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PropertyType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysicalAssetInfraGlobalPropertyRepository extends JpaRepository<PhysicalAssetInfraGlobalProperty, Long> {

    List<PhysicalAssetInfraGlobalProperty> findByInfraType(InfraType infraType);

    List<PhysicalAssetInfraGlobalProperty> findByPropertyType(PropertyType propertyType);

    List<PhysicalAssetInfraGlobalProperty> findByInfraTypeAndPropertyType(InfraType infraType, PropertyType propertyType);

    Optional<PhysicalAssetInfraGlobalProperty> findFirstByInfraTypeAndPropertyType(InfraType infraType, PropertyType propertyType);
}