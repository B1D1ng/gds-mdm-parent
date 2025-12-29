package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.contract.model.Routing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutingRepository extends JpaRepository<Routing, Long> {
    List<Routing> findByContractIdAndContractVersion(Long contractId, Integer contractVersion);
}