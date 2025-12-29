package com.ebay.behavior.gds.mdm.common.model.external.udc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Entity {

    @NotNull
    private UdcEntityType entityType;

    @NotBlank
    @JsonProperty("graphPK")
    private String graphPk;

    @JsonProperty("sourcePK")
    private String sourcePk;

    private String source;

    private Map<String, Object> properties;

    private Map<String, List<String>> relationships;

    private Map<String, Object> edgeProperties;

    private Long timestamp;
    private String jobId;
    private String traceId;
    private boolean deleted;
    private String checksum;
    private String syncStatus;
    private List<String> sources;
    private String latestVersion;
    private String deletedBy;
    private Long deletedDate;
    private boolean performedCheck;
    private boolean performedTransformation;
    private String deleteStatusUpdatedBy;
    private Long deleteStatusUpdatedDate;
}