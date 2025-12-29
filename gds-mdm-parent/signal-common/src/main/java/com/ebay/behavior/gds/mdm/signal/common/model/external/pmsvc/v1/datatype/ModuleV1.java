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
public class ModuleV1 extends PmsvcModelV1 {

    private String ownerEmail;
    private String authoringTool;
    private String createdBy;
    private String lastModifiedBy;
}
