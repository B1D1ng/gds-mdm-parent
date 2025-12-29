package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.Set;

import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.SEGMENT_VALUE_PARAM;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@ToString(exclude = {"fields", "events"})
@EqualsAndHashCode(callSuper = true, exclude = {"fields", "events"})
@Entity
@IdClass(VersionedId.class)
@Table(name = "signal_definition")
public class UnstagedSignal extends MetadataSignal implements Signal, Metadata {

    // The ID column uses custom table generator since GenerationType.IDENTITY is not supported by hibernate for composite keys
    // and GenerationType.SEQUENCE is not supported by mySql
    // Since it must use the custom table generator, it cannot be placed in the superclass
    @Id
    @Column(name = ID)
    @PositiveOrZero
    @GeneratedValue(generator = "custom-table-generator")
    @GenericGenerator(
            name = "custom-table-generator",
            strategy = "com.ebay.behavior.gds.mdm.common.service.CustomTableGenerator",
            parameters = {
                    @Parameter(name = SEGMENT_VALUE_PARAM, value = "signal_seq"),
                    @Parameter(name = INCREMENT_PARAM, value = "5")
            })
    private Long id;

    @Builder.Default
    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment = Environment.UNSTAGED;

    @DiffInclude
    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "signal")
    private Set<UnstagedField> fields;

    // An attempt to make it @ManyToMany with a composite key has failed, because of mapping table entries deleted by hibernate with an unclear reason.
    // So, it was decided not to use hibernate for this relation. Events can be fetched by UnstagedService.getEvents(signalId) method
    @DiffInclude
    @Transient
    private Set<UnstagedEvent> events;

    @Override
    @JsonIgnore
    public VersionedId getSignalId() {
        return VersionedId.of(getId(), getVersion());
    }

    @JsonIgnore
    public UnstagedSignal setSignalId(VersionedId id) {
        setId(id.getId());
        setVersion(id.getVersion());
        return this;
    }
}
