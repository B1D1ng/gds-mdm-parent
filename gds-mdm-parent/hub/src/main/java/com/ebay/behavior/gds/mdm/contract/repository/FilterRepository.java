package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.contract.model.Filter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface FilterRepository extends JpaRepository<Filter, Long> {
    List<Filter> findByComponentId(Long componentId);

    @Modifying
    void deleteAllByComponentId(Long componentId);
}