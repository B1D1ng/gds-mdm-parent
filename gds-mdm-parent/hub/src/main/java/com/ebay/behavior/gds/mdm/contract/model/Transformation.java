package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true, exclude = "component")
@SuperBuilder(toBuilder = true)
@Entity
@EqualsAndHashCode(callSuper = true, exclude = "component")
@Table(name = "transformation")
public class Transformation extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "component_id")
    private Long componentId;

    @NotBlank
    @Column(name = "field")
    private String field;

    @Column(name = "expression_type")
    @Enumerated(EnumType.STRING)
    private ExpressionType expressionType;

    @NotBlank
    @Column(name = "expression")
    private String expression;

    @Column(name = "field_type")
    private String fieldType;

    @NotBlank
    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "owners")
    private String owners;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", insertable = false, updatable = false)
    private Component component;
}