package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.PropertyType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing infrastructure information for physical assets.
 */
@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "dec_physical_asset_infra")
public class PhysicalAssetInfra extends DecAuditable {

    @NotNull
    @Column(name = "infra_type")
    @Enumerated(EnumType.STRING)
    private InfraType infraType;

    @NotNull
    @Column(name = "property_type")
    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;

    @Column(name = "property_details", columnDefinition = "longtext")
    private String propertyDetails;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "platform_environment")
    private PlatformEnvironment platformEnvironment = PlatformEnvironment.STAGING;
}