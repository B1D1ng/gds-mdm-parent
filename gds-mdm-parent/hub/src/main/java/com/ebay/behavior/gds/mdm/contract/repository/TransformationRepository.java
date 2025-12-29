package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.contract.model.Transformation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface TransformationRepository extends JpaRepository<Transformation, Long> {
    List<Transformation> findByComponentId(Long componentId);

    @Modifying
    void deleteAllByComponentId(Long componentId);
}