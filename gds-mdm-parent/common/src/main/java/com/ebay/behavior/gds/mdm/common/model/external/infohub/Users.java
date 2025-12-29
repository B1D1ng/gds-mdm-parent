package com.ebay.behavior.gds.mdm.common.model.external.infohub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Users(boolean success, String msg, List<User> object) {
}
