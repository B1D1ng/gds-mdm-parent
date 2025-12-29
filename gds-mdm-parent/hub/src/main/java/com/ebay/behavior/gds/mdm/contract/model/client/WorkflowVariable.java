package com.ebay.behavior.gds.mdm.contract.model.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowVariable {
    private String environment;
    private String contractId;
    private String pipelineId;
    private String test;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String dlsAppNameId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String dlsNamespaceId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String dlsPipelineId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String needWait;
}
