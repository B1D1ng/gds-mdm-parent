package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.model.SignalChildId;

import java.util.Set;

public interface StagedSignalEventMappingRepositoryCustom {

    Set<SignalChildId> findEventIdsBySignalIds(Set<VersionedId> signalIds);
}
