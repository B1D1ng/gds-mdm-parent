package com.ebay.behavior.gds.mdm.common.model.external.infohub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true, value = {"ExecutiveOrganization"})
public record DedicatedTeam(String name, ArrayList<String> dl, Domain domain) {
}
