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
public class FamilyV1 extends PmsvcModelV1 {

    private String owner;
    private String createdBy;
    private String modifiedBy;
    private int tenantId;
    private String displayName;
}