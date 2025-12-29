package com.ebay.behavior.gds.mdm.udf.repository;

import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubVersions;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UdfStubVersionRepository extends JpaRepository<UdfStubVersions, Long> {
}
