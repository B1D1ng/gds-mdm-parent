package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.DatasetDeployment;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetDeploymentRepository extends JpaRepository<DatasetDeployment, Long> {

    List<DatasetDeployment> findByDatasetIdAndDatasetVersionAndEnvironment(Long datasetId, Integer datasetVersion, PlatformEnvironment environment);

    List<DatasetDeployment> findByDatasetId(Long datasetId);
}
