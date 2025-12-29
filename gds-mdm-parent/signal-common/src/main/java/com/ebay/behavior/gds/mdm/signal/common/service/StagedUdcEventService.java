package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.ElasticsearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;

import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.EVENT;

@Service
@Validated
public class StagedUdcEventService
        extends AbstractStagedUdcService<UnstagedEvent>
        implements ElasticsearchService<UnstagedEvent> {

    @Getter
    private final UdcEntityType entityType = EVENT;

    @Getter
    private final Class<UnstagedEvent> type = UnstagedEvent.class;
}