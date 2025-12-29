package com.ebay.behavior.gds.mdm.udf.repository;

import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface UdfStubRepository extends JpaRepository<UdfStub, Long> {
    List<UdfStub> findByStubNameIn(Set<String> udfNames);
}
