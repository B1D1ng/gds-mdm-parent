package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.DatasetPhysicalStorageMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface DatasetPhysicalStorageMappingRepository
        extends JpaRepository<DatasetPhysicalStorageMapping, Long> {

    @Query(
            "SELECT s FROM DatasetPhysicalStorageMapping s INNER JOIN DatasetIndex v ON s.datasetId = v.id and s.datasetVersion = v.currentVersion "
                    + "WHERE s.datasetId = :datasetId")
    List<DatasetPhysicalStorageMapping> findByDatasetIdCurrentVersion(Long datasetId);

    List<DatasetPhysicalStorageMapping> findByDatasetId(Long datasetId);

    List<DatasetPhysicalStorageMapping> findByPhysicalStorageId(Long physicalStorageId);

    @Query("SELECT mp FROM DatasetPhysicalStorageMapping mp WHERE mp.physicalStorageId IN :storageIds AND mp.datasetId <> :datasetId")
    Set<DatasetPhysicalStorageMapping> findSharedStorage(Set<Long> storageIds, Long datasetId);
}
