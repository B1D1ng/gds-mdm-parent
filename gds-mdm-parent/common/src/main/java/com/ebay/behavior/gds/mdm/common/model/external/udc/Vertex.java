package com.ebay.behavior.gds.mdm.common.model.external.udc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vertex {

    @NotBlank
    private String entityId;

    @NotBlank
    private String entityType;

    private Map<String, String> properties;
}
