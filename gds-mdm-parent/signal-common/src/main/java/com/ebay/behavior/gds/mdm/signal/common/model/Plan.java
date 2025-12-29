package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "plan")
public class Plan extends AbstractAuditable implements Auditable {

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

    @PositiveOrZero
    @Column(name = "platform_id")
    private Long platformId;

    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", insertable = false, updatable = false)
    private PlatformLookup platform;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PlanStatus status;

    @Column(name = "comment")
    private String comment;

    @Transient
    private List<PlanUserAction> operations;

    @JsonIgnore
    public List<String> getOwnersAsList() {
        if (owners == null) {
            return List.of();
        }

        return toList(owners);
    }

    @JsonIgnore
    public Plan withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public Plan withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }
}
