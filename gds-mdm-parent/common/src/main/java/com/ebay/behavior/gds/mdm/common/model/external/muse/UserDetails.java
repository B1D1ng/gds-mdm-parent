package com.ebay.behavior.gds.mdm.common.model.external.muse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserDetails(String status, Data data) {
    public String getUsername() {
        return data().username();
    }
}
