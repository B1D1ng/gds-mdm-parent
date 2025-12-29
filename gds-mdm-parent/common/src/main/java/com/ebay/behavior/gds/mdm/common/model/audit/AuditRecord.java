package com.ebay.behavior.gds.mdm.common.model.audit;

import com.ebay.behavior.gds.mdm.common.model.Auditable;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.diff.Diff;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.UNCHECKED;

@Getter
public class AuditRecord<A extends Auditable> {

    @NotBlank
    private final String uuid;

    @NotBlank
    private final String updateBy;

    private final Timestamp updateDate;

    @PositiveOrZero
    private final int revision;

    @NotNull
    private final ChangeType changeType;

    private final String changeReason;

    @Setter
    private Diff diff;

    @Valid
    private final A left;

    @Valid
    private final A right;

    @Setter
    private List<ChangeLog> changes;

    @JsonCreator
    public AuditRecord(int revision, ChangeType changeType, String changeReason, A left, Diff diff, A right) {
        this.uuid = UUID.randomUUID().toString();
        this.updateBy = right.getUpdateBy();
        this.updateDate = right.getUpdateDate();
        this.revision = revision;
        this.changeType = changeType;
        this.changeReason = changeReason;
        this.left = left;
        this.diff = diff;
        this.right = right;
    }

    @SuppressWarnings(UNCHECKED)
    public AuditRecord(HistoryAuditable curr) {
        this(curr.getOriginalRevision(), curr.getChangeType(), curr.getChangeReason(), null, null, (A) curr);
    }

    @SuppressWarnings(UNCHECKED)
    public AuditRecord(HistoryAuditable prev, Diff diff, HistoryAuditable curr) {
        this(curr.getOriginalRevision(), curr.getChangeType(), curr.getChangeReason(), (A) prev, diff, (A) curr);
    }
}
