package com.ebay.behavior.gds.mdm.signal.model.migration;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "signal_migration_job")
public class SignalMigrationJob extends AbstractAuditable implements Auditable {

    @NotNull
    @Column(name = "job_id")
    private Long jobId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SignalMigrationJobStatus status;
}
