package com.ebay.behavior.gds.mdm.common.model.external.infohub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true, value = {"ProjectContact"})
public record Project(
        String name,
        DedicatedTeam dedicatedTeam,
        boolean status,
        String projectKey) {
}
