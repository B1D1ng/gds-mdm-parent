package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@MappedSuperclass
public abstract class AbstractDimLookup extends AbstractAuditable implements Auditable {

    @NotBlank
    @Column(name = NAME)
    private String name;

    @Column(name = "readable_name")
    private String readableName;

    @NotNull
    @PositiveOrZero
    @Column(name = "dimension_type_id")
    private Long dimensionTypeId;
}
