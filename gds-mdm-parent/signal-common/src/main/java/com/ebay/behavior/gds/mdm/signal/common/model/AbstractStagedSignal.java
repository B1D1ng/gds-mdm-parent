package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.Set;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@MappedSuperclass
@IdClass(VersionedId.class)
public abstract class AbstractStagedSignal extends MetadataSignal implements Signal, Metadata {

    // The ID column is never generated for staged tables, but rather copied from an UnstagedSignal
    @Id
    @Column(name = ID)
    @PositiveOrZero
    private Long id;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment;

    // An attempt to make it @ManyToMany with a composite key has failed, because of mapping table entries deleted by hibernate with an unclear reason.
    // So, it was decided not to use hibernate for this relation. Events can be fetched by StagedSignalService.getEvents(signalId) method
    @DiffInclude
    @Transient
    private Set<StagedEvent> events;

    @Valid
    @Transient
    private UnstagedSignalProxy unstagedSignal;

    @Transient
    private Set<@Valid FieldGroup<StagedField>> fieldGroups;

    @Override
    @JsonIgnore
    public VersionedId getSignalId() {
        return VersionedId.of(getId(), getVersion());
    }

    @JsonIgnore
    public AbstractStagedSignal setSignalId(VersionedId id) {
        setId(id.getId());
        setVersion(id.getVersion());
        return this;
    }

    public abstract void setFields(Set<StagedField> fields);

    @JsonIgnore
    @SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
    public void setUnstagedDetails(UnstagedSignal unstagedSignal, String planName) {
        if (unstagedSignal == null) {
            return;
        }

        boolean isUnstaged = switch (unstagedSignal.getEnvironment()) {
            case UNSTAGED, STAGING -> true;
            case PRODUCTION -> false;
        };

        this.unstagedSignal = new UnstagedSignalProxy(
                isUnstaged,
                unstagedSignal.getPlanId(),
                planName,
                unstagedSignal.getVersion(),
                unstagedSignal.getEnvironment()
        );
    }
}
