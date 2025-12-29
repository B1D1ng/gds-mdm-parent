package com.ebay.behavior.gds.mdm.signal.common.model.audit;

import com.ebay.behavior.gds.mdm.common.model.audit.AbstractHistoryAuditable;
import com.ebay.behavior.gds.mdm.common.model.audit.ChangeType;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "plan_history")
public class PlanHistory extends AbstractHistoryAuditable {

    @NotBlank
    @Column(name = NAME)
    private String name;

    @NotBlank
    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "team_dls")
    private String teamDls;

    @NotBlank
    @Column(name = "owners")
    private String owners;

    @NotBlank
    @Column(name = "jira_project")
    private String jiraProject;

    @NotBlank
    @Column(name = "domain")
    private String domain;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PlanStatus status;

    @Column(name = "comment")
    private String comment;

    @JsonIgnore
    public PlanHistory withOriginalId(Long id) {
        return this.toBuilder().originalId(id).build();
    }

    @JsonIgnore
    public PlanHistory withOriginalRevision(Integer revision) {
        return this.toBuilder().originalRevision(revision).build();
    }

    @JsonIgnore
    public PlanHistory withType(ChangeType changeType) {
        return this.toBuilder().changeType(changeType).build();
    }
}
