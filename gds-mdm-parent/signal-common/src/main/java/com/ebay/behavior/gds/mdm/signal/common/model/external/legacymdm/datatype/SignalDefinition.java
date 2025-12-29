package com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignalDefinition {

    @NotBlank
    private String id;

    @NotNull
    private Integer version;

    @NotBlank
    private String domain;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    private String type;

    @DiffIgnore
    private Long status;

    private Integer refVersion;

    private String createdUser;

    @DiffIgnore
    private String createdTime;

    @DiffIgnore
    private String updatedTime;

    private String updatedUser;

    @NotEmpty
    private List<LogicalDefinition> logicalDefinition;
}
