package com.ebay.behavior.gds.mdm.signal.common.model.audit;

import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.audit.AbstractHistoryAuditable;
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
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "signal_template_history")
public class SignalTemplateHistory extends AbstractHistoryAuditable {

    @NotBlank
    @Column(name = NAME)
    private String name;

    @NotBlank
    @Column(name = "description")
    private String description;

    @Column(name = "domain")
    private String domain;

    @NotBlank
    @Column(name = "type")
    private String type;

    @Column(name = "retention_period")
    private Long retentionPeriod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status")
    private CompletionStatus completionStatus;

    @PositiveOrZero
    @Column(name = "platform_id")
    private Long platformId;

    @JsonIgnore
    public SignalTemplateHistory withOriginalId(Long id) {
        return this.toBuilder().originalId(id).build();
    }

    @JsonIgnore
    public SignalTemplateHistory withOriginalRevision(Integer revision) {
        return this.toBuilder().originalRevision(revision).build();
    }

    @JsonIgnore
    public SignalTemplateHistory withType(ChangeType changeType) {
        return this.toBuilder().changeType(changeType).build();
    }
}
