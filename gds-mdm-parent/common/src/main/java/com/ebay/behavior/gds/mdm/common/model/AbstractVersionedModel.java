package com.ebay.behavior.gds.mdm.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@MappedSuperclass
public abstract class AbstractVersionedModel implements VersionedModel {

    @Id
    @Positive
    @Column(name = VERSION)
    private Integer version;

    @Version
    @PositiveOrZero
    @Column(name = REVISION)
    private Integer revision;
}
