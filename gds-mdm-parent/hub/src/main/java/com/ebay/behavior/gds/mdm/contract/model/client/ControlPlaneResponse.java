package com.ebay.behavior.gds.mdm.contract.model.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

@Data
public class ControlPlaneResponse {
    private Long id;

    private String status;

    private String resourceType;

    private String resourceId;

    private String processType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String workflowId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String workerGroup;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> context;

    private String requester;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String createdTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String modifiedTime;
}
