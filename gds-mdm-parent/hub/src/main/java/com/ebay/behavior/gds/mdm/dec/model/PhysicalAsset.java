package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.dec.model.enums.DecEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.PhysicalAssetType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.List;
import java.util.Set;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true, exclude = {"ldmIds", "assetInfras", "attributes"})
@Table(name = "dec_physical_asset")
public class PhysicalAsset extends DecAuditable {

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type")
    private PhysicalAssetType assetType;

    @NotNull
    @DiffInclude
    @Column(name = "asset_name")
    private String assetName;

    @DiffInclude
    @Column(name = "asset_details")
    private String assetDetails;

    @DiffInclude
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_environment")
    private PlatformEnvironment storageEnvironment = PlatformEnvironment.STAGING;

    @Transient
    private Set<Long> ldmIds;

    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "dec_environment")
    private DecEnvironment decEnvironment;

    @OneToMany(fetch = FetchType.LAZY)
    @JsonSerialize(using = LazyObjectSerializer.class)
    @JoinTable(
            name = "dec_physical_asset_infra_mapping",
            joinColumns = @JoinColumn(name = "asset_id", referencedColumnName = ID, insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "asset_infra_id", referencedColumnName = ID, insertable = false, updatable = false)
    )
    private Set<PhysicalAssetInfra> assetInfras;
    
    @Transient
    private List<PhysicalAssetAttribute> attributes;
}
