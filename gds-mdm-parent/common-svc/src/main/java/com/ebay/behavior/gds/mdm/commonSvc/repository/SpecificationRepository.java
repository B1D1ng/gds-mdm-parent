package com.ebay.behavior.gds.mdm.commonSvc.repository;

import com.ebay.behavior.gds.mdm.common.model.Auditable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SpecificationRepository<M extends Auditable, K> extends JpaRepository<M, K> {
    Page<M> findAll(Specification<M> spec, Pageable pageable);
}
