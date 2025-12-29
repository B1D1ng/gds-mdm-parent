package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.contract.model.ContractPipeline;
import com.ebay.behavior.gds.mdm.contract.model.DeployScope;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractPipelineRepository extends JpaRepository<ContractPipeline, Long> {
    List<ContractPipeline> findByContractIdAndContractVersion(Long contractId, Integer contractVersion);

    @Query("SELECT p FROM ContractPipeline p WHERE p.contractId = :contractId AND p.contractVersion = :contractVersion "
            + "AND p.environment = :environment")
    Optional<ContractPipeline> findPipeline(@Param("contractId") long contractId,
                                            @Param("contractVersion") int contractVersion,
                                            @Param("environment") Environment environment);

    @Query("SELECT p FROM ContractPipeline p WHERE p.contractId = :contractId AND p.contractVersion = :contractVersion "
            + "AND p.environment = :environment AND p.deployScope = :deployScope")
    Optional<ContractPipeline> findPipeline(@Param("contractId") long contractId,
                                            @Param("contractVersion") int contractVersion,
                                            @Param("environment") Environment environment,
                                            @Param("deployScope") DeployScope deployScope);
}