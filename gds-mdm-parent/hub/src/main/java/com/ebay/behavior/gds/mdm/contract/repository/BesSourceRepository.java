package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.commonSvc.repository.SpecificationRepository;
import com.ebay.behavior.gds.mdm.contract.model.BesSource;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BesSourceRepository extends JpaRepository<BesSource, Long>, SpecificationRepository<BesSource, Long> {
}