package com.ebay.behavior.gds.mdm.dec.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class LdmEntityInitialization {

    private String name;

    private String description;

    private String owners;

    private String jiraProject;

    private String domain;

    private String pk;

    private Long namespaceId;
}
