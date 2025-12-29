package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.Set;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString(exclude = "attributes")
@EqualsAndHashCode(callSuper = true, exclude = "attributes")
@Entity
@Table(name = "staged_event")
public class StagedEvent extends MetadataEvent implements Event, Metadata {

    @DiffInclude
    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event")
    private Set<StagedAttribute> attributes;

    @DiffInclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "staged_event_page_map", joinColumns = @JoinColumn(name = "event_id"))
    @BatchSize(size = 100)
    @Column(name = "page_id")
    private Set<Long> pageIds;

    @DiffInclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "staged_event_module_map", joinColumns = @JoinColumn(name = "event_id"))
    @BatchSize(size = 100)
    @Column(name = "module_id")
    private Set<Long> moduleIds;

    @DiffInclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "staged_event_click_map", joinColumns = @JoinColumn(name = "event_id"))
    @BatchSize(size = 100)
    @Column(name = "click_id")
    private Set<Long> clickIds;
}