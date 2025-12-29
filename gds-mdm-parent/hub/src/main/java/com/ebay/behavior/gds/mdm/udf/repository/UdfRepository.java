package com.ebay.behavior.gds.mdm.udf.repository;

import com.ebay.behavior.gds.mdm.udf.common.model.Udf;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface UdfRepository extends JpaRepository<Udf, Long> {
    List<Udf> findByNameIn(Set<String> udfNames);
}
