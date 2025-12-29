package com.ebay.behavior.gds.mdm.dec.model.manyToMany;

import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "dec_physical_asset_ldm_mapping")
public class PhysicalAssetLdmMapping {

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "asset_id", referencedColumnName = ID)
    private PhysicalAsset physicalAsset;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "ldm_base_entity_id", referencedColumnName = ID)
    private LdmBaseEntity ldmBaseEntity;

    public PhysicalAssetLdmMapping(PhysicalAsset physicalAsset, LdmBaseEntity ldmBaseEntity) {
        this.physicalAsset = physicalAsset;
        this.ldmBaseEntity = ldmBaseEntity;
    }
}