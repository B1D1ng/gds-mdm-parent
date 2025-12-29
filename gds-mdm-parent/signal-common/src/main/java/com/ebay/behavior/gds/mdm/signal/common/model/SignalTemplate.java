package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static jakarta.persistence.FetchType.LAZY;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(exclude = {"fields", "events"})
@EqualsAndHashCode(callSuper = true, exclude = {"fields", "events"})
@Entity
@Table(name = "signal_template")
public class SignalTemplate extends AbstractAuditable implements Signal {

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
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status")
    private CompletionStatus completionStatus = CompletionStatus.DRAFT;

    @PositiveOrZero
    @Column(name = "platform_id")
    private Long platformId;

    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "platform_id", insertable = false, updatable = false)
    private PlatformLookup platform;

    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = LAZY, mappedBy = "signal")
    private Set<FieldTemplate> fields;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "signal_event_template_map",
            joinColumns = @JoinColumn(name = "signal_id", referencedColumnName = ID, insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "event_id", referencedColumnName = ID, insertable = false, updatable = false)
    )
    private Set<EventTemplate> events;

    @JsonIgnore
    public SignalTemplate withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public SignalTemplate withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }

    @Override
    @JsonIgnore
    public VersionedId getSignalId() {
        return VersionedId.of(getId(), -1); // there is no version for templates, but common Signal API requires a method with VersionedId
    }

    @Override
    @JsonIgnore
    public Long getSignalTemplateSourceId() {
        return getId();
    }
}
