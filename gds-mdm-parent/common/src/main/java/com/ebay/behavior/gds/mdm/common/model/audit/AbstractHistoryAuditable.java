package com.ebay.behavior.gds.mdm.common.model.audit;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.sql.Timestamp;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@MappedSuperclass
public abstract class AbstractHistoryAuditable extends AbstractAuditable implements HistoryAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "original_id")
    private Long originalId;

    @NotNull
    @PositiveOrZero
    @Column(name = "original_revision")
    @DiffIgnore
    private Integer originalRevision;

    @Column(name = "original_create_date")
    @DiffIgnore
    private Timestamp originalCreateDate;

    @Column(name = "original_update_date")
    @DiffIgnore
    private Timestamp originalUpdateDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type")
    @DiffIgnore
    private ChangeType changeType;

    @Column(name = "change_reason")
    @DiffIgnore
    private String changeReason;
}
