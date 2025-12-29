package com.ebay.behavior.gds.mdm.contract.repository.manyToMany;

import com.ebay.behavior.gds.mdm.contract.model.manyToMany.RoutingComponentMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface RoutingComponentMappingRepository extends JpaRepository<RoutingComponentMapping, Long> {
    List<RoutingComponentMapping> findByComponentId(Long componentId);

    @Modifying
    void deleteAllByRoutingId(Long routingId);
}