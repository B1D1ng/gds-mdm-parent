package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.ElasticsearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;

import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.FIELD;

@Service
@Validated
public class StagedUdcFieldService
        extends AbstractStagedUdcService<UnstagedField>
        implements ElasticsearchService<UnstagedField> {

    @Getter
    private final UdcEntityType entityType = FIELD;

    @Getter
    private final Class<UnstagedField> type = UnstagedField.class;
}
