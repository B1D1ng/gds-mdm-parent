package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(exclude = "event")
@EqualsAndHashCode(callSuper = true, exclude = "event")
@Entity
@Table(name = "attribute_template")
public class AttributeTemplate extends AbstractAuditable implements Attribute {

    @NotNull
    @PositiveOrZero
    @Column(name = "event_template_id")
    private Long eventTemplateId;

    @NotBlank
    @Column(name = "tag")
    private String tag;

    @NotBlank
    @Column(name = "description")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "java_type")
    private JavaType javaType;

    @NotBlank
    @Column(name = "schema_path")
    private String schemaPath;

    @Builder.Default
    @Column(name = "is_store_in_state")
    private Boolean isStoreInState = false;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_template_id", insertable = false, updatable = false)
    private EventTemplate event;

    @Override
    @JsonIgnore
    public Long getParentId() {
        return eventTemplateId;
    }

    @JsonIgnore
    public AttributeTemplate withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public AttributeTemplate withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }
}