package com.ebay.behavior.gds.mdm.udf.repository;

import com.ebay.behavior.gds.mdm.udf.common.model.UdfVersions;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UdfVersionRepository extends JpaRepository<UdfVersions,Long> {
}
