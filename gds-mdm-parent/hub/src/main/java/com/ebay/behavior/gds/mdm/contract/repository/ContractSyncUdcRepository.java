package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.contract.model.ContractSyncUdc;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractSyncUdcRepository extends JpaRepository<ContractSyncUdc, Long> {
    ContractSyncUdc findByContractId(long contractId);
}