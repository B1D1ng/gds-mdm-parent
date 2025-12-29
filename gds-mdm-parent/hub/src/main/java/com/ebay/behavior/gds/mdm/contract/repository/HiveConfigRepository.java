package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface HiveConfigRepository extends JpaRepository<HiveConfig, Long> {
    List<HiveConfig> getHiveConfigByComponentId(@NotNull @PositiveOrZero Long componentId);

    @Modifying
    void deleteAllByComponentId(Long componentId);
}
