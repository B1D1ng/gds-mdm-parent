package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.serde.AvroSchemaDeserializer;
import com.ebay.behavior.gds.mdm.common.serde.AvroSchemaSerializer;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.signal.common.model.converter.AvroSchemaConverter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.apache.avro.Schema;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(exclude = {"signal", "attributes", "eventTypes"})
@EqualsAndHashCode(callSuper = true, exclude = {"signal", "attributes", "eventTypes"})
@Entity
@Table(name = "field_template")
public class FieldTemplate extends AbstractAuditable implements Field {

    @NotNull
    @PositiveOrZero
    @Column(name = "signal_template_id")
    private Long signalTemplateId;

    @NotBlank
    @Column(name = NAME)
    private String name;

    @NotBlank
    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "tag")
    private String tag;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "java_type")
    private JavaType javaType;

    @NotNull
    @Column(name = "avro_schema")
    @Convert(converter = AvroSchemaConverter.class)
    @JsonSerialize(using = AvroSchemaSerializer.class)
    @JsonDeserialize(using = AvroSchemaDeserializer.class)
    private Schema avroSchema;

    @Column(name = "expression")
    private String expression;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "expression_type")
    private ExpressionType expressionType;

    @NotNull
    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    @Column(name = "is_cached")
    private Boolean isCached;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_template_id", insertable = false, updatable = false)
    private SignalTemplate signal;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "field_attribute_template_map",
            joinColumns = @JoinColumn(name = "field_id", referencedColumnName = ID, insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "attribute_id", referencedColumnName = ID, insertable = false, updatable = false)
    )
    private Set<AttributeTemplate> attributes;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_type_field_template_map",
            joinColumns = @JoinColumn(name = "field_id", referencedColumnName = ID, insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "event_type_id", referencedColumnName = ID, insertable = false, updatable = false)
    )
    private Set<EventTypeLookup> eventTypes;

    @Override
    @JsonIgnore
    public String getEventTypesAsString() {
        return String.join(COMMA, getEventTypes().stream().map(EventTypeLookup::getName).toList());
    }

    @Override
    public Long getParentId() {
        return signalTemplateId;
    }

    @JsonIgnore
    public FieldTemplate withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public FieldTemplate withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }
}
