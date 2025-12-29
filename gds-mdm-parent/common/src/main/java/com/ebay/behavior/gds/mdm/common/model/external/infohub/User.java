package com.ebay.behavior.gds.mdm.common.model.external.infohub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record User(String loginName, String firstName, String lastName, String email) {
}
