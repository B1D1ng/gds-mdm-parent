package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;
import java.util.Set;

public interface MetadataWriteService {

    String deleteSignal(@PositiveOrZero long id);

    String deleteField(@PositiveOrZero long id);

    String deleteEvent(@PositiveOrZero long id);

    String deleteAttribute(@PositiveOrZero long id);

    Map<UdcEntityType, String> upsertSignal(Set<Long> eventIds, @Valid @NotNull UnstagedSignal signal);

    Map<UdcEntityType, String> upsertField(@PositiveOrZero long signalId, Set<Long> attributeIds, @Valid @NotNull UnstagedField field);

    String upsertEvent(@Valid @NotNull UnstagedEvent event);

    String upsertAttribute(@PositiveOrZero long eventId, @Valid @NotNull UnstagedAttribute attribute);

    String upsert(@Valid @NotNull UnstagedSignal signal, @NotNull UdcDataSourceType dataSource);
}
