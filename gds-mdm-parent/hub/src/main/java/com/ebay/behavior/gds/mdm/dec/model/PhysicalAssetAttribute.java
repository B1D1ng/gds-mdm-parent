package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.dec.model.enums.PhysicalAssetAttributeName;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"physicalAsset"})
@EqualsAndHashCode(callSuper = true, exclude = {"physicalAsset"})
@Table(name = "dec_physical_asset_attributes")
public class PhysicalAssetAttribute extends DecAuditable {

    // Added for JSON serialization
    @Column(name = "asset_id", insertable = false, updatable = false)
    private Long assetId;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "attribute_name")
    private PhysicalAssetAttributeName attributeName;

    @NotNull
    @DiffInclude
    @Column(name = "attribute_value")
    private String attributeValue;
}
