package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.Set;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString(exclude = {"fields", "events", "fieldGroups"})
@EqualsAndHashCode(callSuper = true, exclude = {"fields", "events", "fieldGroups"})
@Entity
@Table(name = "staged_signal")
public class StagedSignal extends AbstractStagedSignal implements Signal, Metadata {

    @DiffInclude
    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "signal")
    private Set<StagedField> fields;
}
