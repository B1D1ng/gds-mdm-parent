package com.ebay.behavior.gds.mdm.common.model.external.muse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Data(String username) {
}
