package com.ebay.behavior.gds.mdm.signal.common.model.audit;

import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.audit.AbstractVersionedHistoryAuditable;
import com.ebay.behavior.gds.mdm.common.model.audit.ChangeType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "signal_history")
public class UnstagedSignalHistory extends AbstractVersionedHistoryAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "plan_id")
    private Long planId;

    @PositiveOrZero
    @Column(name = "signal_template_source_id")
    private Long signalTemplateSourceId;

    @PositiveOrZero
    @Column(name = "signal_source_id")
    private Long signalSourceId;

    @NotBlank
    @Column(name = NAME)
    private String name;

    @NotBlank
    @Column(name = "description")
    private String description;

    @Column(name = "domain")
    private String domain;

    @Column(name = "owners")
    private String owners;

    @NotBlank
    @Column(name = "type")
    private String type;

    @Column(name = "retention_period")
    private Long retentionPeriod;

    @PositiveOrZero
    @Column(name = "platform_id")
    private Long platformId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status")
    private CompletionStatus completionStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment;

    @JsonIgnore
    public UnstagedSignalHistory withOriginalId(Long id) {
        return this.toBuilder().originalId(id).build();
    }

    @JsonIgnore
    public UnstagedSignalHistory withOriginalRevision(Integer revision) {
        return this.toBuilder().originalRevision(revision).build();
    }

    @JsonIgnore
    public UnstagedSignalHistory withType(ChangeType changeType) {
        return this.toBuilder().changeType(changeType).build();
    }
}
