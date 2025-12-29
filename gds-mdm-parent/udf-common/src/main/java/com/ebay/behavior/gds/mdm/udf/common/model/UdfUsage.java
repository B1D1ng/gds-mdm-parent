package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfUsageType;

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
@ToString(exclude = {"udf"})
@EqualsAndHashCode(callSuper = true, exclude = {"udf"})
@Entity
@Table(name = "udf_usage")
public class UdfUsage extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "udf_id")
    private Long udfId;

    @Column(name = "usage_type")
    @Enumerated(EnumType.STRING)
    private UdfUsageType usageType;

    @NotBlank
    @Column(name = "udc_id")
    private String udcId;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "udf_id", insertable = false, updatable = false)
    private Udf udf;

    @JsonIgnore
    public UdfUsage withId(Long id) {
        return this.toBuilder().id(id).build();
    }
}
