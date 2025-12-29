package com.ebay.behavior.gds.mdm.common.model.external.udc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LineageResponse {

    @NotBlank
    private String apiVersion;

    @NotBlank
    private String kind;

    @Valid
    @NotNull
    private LineageData data;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineageData {
        private List<Vertex> vertices;
        private List<Edge> edges;
    }
}
