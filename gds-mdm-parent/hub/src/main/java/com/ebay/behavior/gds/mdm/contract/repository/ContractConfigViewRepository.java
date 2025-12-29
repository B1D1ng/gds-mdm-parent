package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.contract.model.ContractConfigView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository for querying unstaged contracts by streaming topics.
 * Uses the contract_streaming_topic_view for optimized read performance.
 * Supports dynamic filtering via JpaSpecificationExecutor.
 */
public interface ContractConfigViewRepository
        extends JpaRepository<ContractConfigView, VersionedId>,
        JpaSpecificationExecutor<ContractConfigView> {

    @Override
    Page<ContractConfigView> findAll(Specification<ContractConfigView> spec, Pageable pageable);
}
