package com.ebay.behavior.gds.mdm.commonSvc.repository;

import com.ebay.behavior.gds.mdm.common.model.AbstractLookup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.Set;

@NoRepositoryBean
public interface LookupRepository<T extends AbstractLookup> extends JpaRepository<T, Long> {

    Optional<T> findByName(String name);

    Set<T> findByNameIn(Set<String> names);
}
