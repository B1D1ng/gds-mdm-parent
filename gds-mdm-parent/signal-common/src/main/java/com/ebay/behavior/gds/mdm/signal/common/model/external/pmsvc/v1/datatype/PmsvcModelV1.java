package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class PmsvcModelV1 {

    private Long id;

    private String name;

    private String description;

    private Integer status;

    @JsonProperty("teamDL")
    private String teamDl;

    private String extension;

    private Date creationDate;

    private Date lastModifiedDate;
}
