package com.ebay.behavior.gds.mdm.contract.repository.manyToMany;

import com.ebay.behavior.gds.mdm.contract.model.manyToMany.ContractSignalMapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractSignalMappingRepository extends JpaRepository<ContractSignalMapping, Long> {
    Optional<ContractSignalMapping> findByContractIdAndContractVersion(Long contractId, Integer contractVersion);
}