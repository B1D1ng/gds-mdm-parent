package com.ebay.behavior.gds.mdm.common.model;

import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

import java.util.Set;

@With
@Getter
@AllArgsConstructor
public class LineageParameters {

    private UdcEntityType entityType;

    private UdcEntityType inputEntityType;
    private String inputEntityIdName;
    private long inputEntityId;
    private String inputRelationType;

    private UdcEntityType outputEntityType;
    private String outputEntityIdName;
    private Set<Long> outputEntityIds;
    private String outputRelationType;

    public LineageParameters(UdcEntityType entityType, Metadata inputMetadata, String inputRelationType,
                             Metadata outputMetadataTemplate, Set<Long> outputEntityIds, String outputRelationType) {
        this.entityType = entityType;
        this.inputEntityType = inputMetadata.getEntityType();
        this.inputEntityIdName = inputMetadata.getEntityType().getIdName();
        this.inputEntityId = inputMetadata.getId();
        this.inputRelationType = inputRelationType;
        this.outputEntityType = outputMetadataTemplate.getEntityType();
        this.outputEntityIdName = outputMetadataTemplate.getEntityType().getIdName();
        this.outputEntityIds = outputEntityIds;
        this.outputRelationType = outputRelationType;
    }
}
