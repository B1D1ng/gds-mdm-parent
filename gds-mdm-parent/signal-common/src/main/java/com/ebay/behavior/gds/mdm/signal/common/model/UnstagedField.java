package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
@ToString(exclude = {"signal", "attributes"})
@EqualsAndHashCode(callSuper = true, exclude = {"signal", "attributes"})
@Entity
@Table(name = "field")
public class UnstagedField extends MetadataField implements Field, Metadata {

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "signal_id", insertable = false, updatable = false),
            @JoinColumn(name = "signal_version", insertable = false, updatable = false)
    })
    private UnstagedSignal signal;

    @DiffInclude
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "field_attribute_map",
            joinColumns = @JoinColumn(name = "field_id", referencedColumnName = ID, insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "attribute_id", referencedColumnName = ID, insertable = false, updatable = false)
    )
    private Set<UnstagedAttribute> attributes;

    @Override
    @JsonIgnore
    public String getEventTypesAsString() {
        return getEventTypes();
    }
}