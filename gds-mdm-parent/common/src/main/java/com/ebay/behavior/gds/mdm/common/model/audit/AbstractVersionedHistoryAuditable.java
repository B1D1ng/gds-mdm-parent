package com.ebay.behavior.gds.mdm.common.model.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffIgnore;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@MappedSuperclass
public abstract class AbstractVersionedHistoryAuditable extends AbstractHistoryAuditable implements VersionedHistoryAuditable {

    @NotNull
    @Positive
    @DiffIgnore
    @Column(name = "original_version")
    private Integer originalVersion;
}
