package com.ebay.behavior.gds.mdm.contract.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@Embeddable
public class UdfAlias {
    @NotBlank
    @Column(name = "name")
    private String name;

    @NotBlank
    @Column(name = "func")
    private String func;

    @NotBlank
    @Column(name = "alias")
    private String alias;
}