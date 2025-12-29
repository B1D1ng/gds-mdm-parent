package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.contract.model.ConfigStorageMapping;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigStorageMappingRepository extends JpaRepository<ConfigStorageMapping, Long> {

    ConfigStorageMapping findByConfigId(Long configId);

    ConfigStorageMapping findByStorageId(Long storageId);
}