package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SignalTypePhysicalStorageMapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface SignalTypePhysicalStorageMappingRepository extends JpaRepository<SignalTypePhysicalStorageMapping, Long> {

    Optional<SignalTypePhysicalStorageMapping> findBySignalTypeIdAndPhysicalStorageId(Long signalTypeId, Long physicalStorageId);

    Set<SignalTypePhysicalStorageMapping> findBySignalTypeId(Long signalTypeId);
}