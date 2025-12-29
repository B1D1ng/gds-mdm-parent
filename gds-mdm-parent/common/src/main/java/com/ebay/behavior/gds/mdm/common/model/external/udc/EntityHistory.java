package com.ebay.behavior.gds.mdm.common.model.external.udc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityHistory {

    private List<@Valid EntityVersion> versions;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EntityVersion {

        @Valid
        @NotNull
        private Entity entityVersionData;

        @Positive
        @NotNull
        private Long version;
    }
}
