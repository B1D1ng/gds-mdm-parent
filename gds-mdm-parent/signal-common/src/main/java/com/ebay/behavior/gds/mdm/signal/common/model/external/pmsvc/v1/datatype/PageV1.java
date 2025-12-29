package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class PageV1 extends PmsvcModelV1 {

    private String ownerEmail;
    private String authoringTool;
    private Boolean inframe;
    private Integer trackingMechanism;
    private String aliasName;
    private Integer isDynamicPage;
    private String urlSample;
    private Integer roiEnabled;
    private String httpMethod;
    private Long pulsarOriginId;
    private Long flagsetId;
    private Long pageGroupId;
}
