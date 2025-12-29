package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.ElasticsearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;

import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.ATTRIBUTE;

@Service
@Validated
public class StagedUdcAttributeService
        extends AbstractStagedUdcService<UnstagedAttribute>
        implements ElasticsearchService<UnstagedAttribute> {

    @Getter
    private final UdcEntityType entityType = ATTRIBUTE;

    @Getter
    private final Class<UnstagedAttribute> type = UnstagedAttribute.class;
}