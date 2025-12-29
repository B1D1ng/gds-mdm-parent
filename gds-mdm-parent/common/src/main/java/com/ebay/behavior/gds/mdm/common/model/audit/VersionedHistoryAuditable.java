package com.ebay.behavior.gds.mdm.common.model.audit;

public interface VersionedHistoryAuditable extends HistoryAuditable {

    Integer getOriginalVersion();

    void setOriginalVersion(Integer version);
}
