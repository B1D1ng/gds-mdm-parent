package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.SurfaceType;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@ToString(exclude = "attributes")
@EqualsAndHashCode(callSuper = true, exclude = "attributes")
@Entity
@Table(name = "event_template")
public class EventTemplate extends AbstractAuditable implements Event {

    @NotBlank
    @Column(name = NAME)
    private String name;

    @NotBlank
    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "type")
    private String type;

    @NotNull
    @Column(name = "source")
    private EventSource source;

    @NotNull
    @Builder.Default
    @Column(name = "fsm_order")
    private Integer fsmOrder = 99_999;

    @NotNull
    @Builder.Default
    @PositiveOrZero
    @Column(name = "cardinality")
    private Integer cardinality = 1;

    @Column(name = "surface_type")
    private SurfaceType surfaceType;

    @Column(name = "expression")
    private String expression;

    @Enumerated(EnumType.STRING)
    @Column(name = "expression_type")
    private ExpressionType expressionType;

    @NotNull
    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = LAZY, mappedBy = "event")
    private Set<AttributeTemplate> attributes;

    @JsonIgnore
    public EventTemplate withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public EventTemplate withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }
}