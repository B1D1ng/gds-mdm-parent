package com.ebay.behavior.gds.mdm.contract.model.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ControlPlaneRequest<CTX> {
    private String resourceType;

    private String resourceId;

    private String processType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CTX context;

    private String requester;
}
