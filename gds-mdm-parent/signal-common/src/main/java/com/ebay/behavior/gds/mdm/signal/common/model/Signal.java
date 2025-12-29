package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;

import java.util.Set;

public interface Signal extends Auditable {

    String getName();

    String getDescription();

    String getDomain();

    String getType();

    Long getRetentionPeriod();

    CompletionStatus getCompletionStatus();

    Set<? extends Event> getEvents();

    Set<? extends Field> getFields();

    VersionedId getSignalId();

    Long getSignalTemplateSourceId();
}
