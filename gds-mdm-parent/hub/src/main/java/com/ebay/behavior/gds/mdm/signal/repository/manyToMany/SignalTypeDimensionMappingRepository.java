package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SignalTypeDimensionMapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface SignalTypeDimensionMappingRepository extends JpaRepository<SignalTypeDimensionMapping, Long> {

    Optional<SignalTypeDimensionMapping> findBySignalTypeIdAndDimensionId(Long signalTypeId, Long dimId);

    Set<SignalTypeDimensionMapping> findBySignalTypeId(Long signalTypeId);
}