package com.ebay.behavior.gds.mdm.contract.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@Embeddable
public class ColumnInfo {

    @NotBlank
    @Column(name = "name")
    private String columnName;

    @NotBlank
    @Column(name = "type")
    private String columnType;

    @Column(name = "comment")
    private String columnComment;

    @Column(name = "extra_opts")
    private String extraOpts;
}
