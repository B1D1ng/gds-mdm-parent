package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dec_ldm_field_physical_storage_mapping")
public class LdmFieldPhysicalStorageMapping extends DecAuditable {

    @NotNull
    @Column(name = "ldm_field_id")
    private Long ldmFieldId;

    @DiffInclude
    @NotNull
    @Column(name = "physical_storage_id")
    private Long physicalStorageId;

    @DiffInclude
    @Column(name = "physical_field_expression")
    private String physicalFieldExpression;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ldm_field_id", insertable = false, updatable = false)
    private LdmField field;
}
