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
@Entity
@EqualsAndHashCode(callSuper = true, exclude = "component")
@SuperBuilder(toBuilder = true)
@Table(name = "filter")
public class Filter extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "component_id")
    private Long componentId;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ExpressionType type;

    @NotNull
    @Column(name = "statement")
    private String statement;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", insertable = false, updatable = false)
    private Component component;
}