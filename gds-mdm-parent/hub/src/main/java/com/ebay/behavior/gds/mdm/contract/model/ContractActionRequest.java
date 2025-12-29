package com.ebay.behavior.gds.mdm.contract.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractActionRequest {
    @NotBlank
    private String comment;
}
