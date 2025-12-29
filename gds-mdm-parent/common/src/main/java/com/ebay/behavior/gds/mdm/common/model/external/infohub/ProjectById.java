package com.ebay.behavior.gds.mdm.common.model.external.infohub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true, value = {"ExecutiveOrganization", "ProjectContact"})
public record ProjectById(boolean success, String msg, Project object) {
}
