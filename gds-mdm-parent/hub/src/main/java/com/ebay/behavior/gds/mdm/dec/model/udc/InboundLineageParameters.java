package com.ebay.behavior.gds.mdm.dec.model.udc;

import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

import java.util.Set;

@With
@Getter
@AllArgsConstructor
public class InboundLineageParameters {

    private UdcEntityType entityType;

    private UdcEntityType inputEntityType;
    private String inputEntityIdName;
    private Set<Long> inputEntityIds;
    private String inputRelationType;

    private UdcEntityType outputEntityType;
    private String outputEntityIdName;
    private String outputEntityId;
    private String outputRelationType;

    public InboundLineageParameters(UdcEntityType entityType, Set<Long> inputEntityIds, String inputRelationType,
                                    UdcEntityType inputEntityType, UdcEntityType outputEntityType, String outputEntityId, String outputRelationType) {
        this.entityType = entityType;
        this.inputEntityType = inputEntityType;
        this.inputEntityIdName = inputEntityType.getIdName();
        this.inputEntityIds = inputEntityIds;
        this.inputRelationType = inputRelationType;

        this.outputEntityType = outputEntityType;
        this.outputEntityIdName = outputEntityType.getIdName();
        this.outputEntityId = outputEntityId;
        this.outputRelationType = outputRelationType;
    }
}
