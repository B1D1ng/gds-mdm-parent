package com.ebay.behavior.gds.mdm.dec.model.manyToMany;

import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing the mapping between PhysicalAsset and PhysicalAssetInfra.
 */
@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode
@Table(name = "dec_physical_asset_infra_mapping")
public class PhysicalAssetInfraMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "asset_id", referencedColumnName = "id")
    private PhysicalAsset physicalAsset;

    @ManyToOne
    @JoinColumn(name = "asset_infra_id", referencedColumnName = "id")
    private PhysicalAssetInfra physicalAssetInfra;

    public PhysicalAssetInfraMapping(PhysicalAsset physicalAsset, PhysicalAssetInfra physicalAssetInfra) {
        this.physicalAsset = physicalAsset;
        this.physicalAssetInfra = physicalAssetInfra;
    }
}
