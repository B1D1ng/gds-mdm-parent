package com.ebay.behavior.gds.mdm.common.model.audit;

import com.ebay.behavior.gds.mdm.common.model.Auditable;

import java.sql.Timestamp;

public interface HistoryAuditable extends Auditable {

    void setId(Long id);

    Long getOriginalId();

    void setOriginalId(Long id);

    Integer getOriginalRevision();

    void setOriginalRevision(Integer revision);

    Timestamp getOriginalCreateDate();

    void setOriginalCreateDate(Timestamp date);

    Timestamp getOriginalUpdateDate();

    void setOriginalUpdateDate(Timestamp date);

    ChangeType getChangeType();

    void setChangeType(ChangeType type);

    String getChangeReason();

    void setChangeReason(String type);
}
