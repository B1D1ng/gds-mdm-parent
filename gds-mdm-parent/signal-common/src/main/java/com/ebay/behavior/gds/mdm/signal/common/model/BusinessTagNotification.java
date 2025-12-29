package com.ebay.behavior.gds.mdm.signal.common.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTagNotification {

    @NotBlank
    private String jobName;

    @NotBlank
    private String table;

    @NotBlank
    private String runDate;

    @NotBlank
    private String status;
}
