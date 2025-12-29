package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.model.SignalChildId;

import java.util.Set;

public interface StagedFieldRepositoryCustom {

    Set<SignalChildId> findFieldIdsBySignalIds(Set<VersionedId> signalIds);
}
