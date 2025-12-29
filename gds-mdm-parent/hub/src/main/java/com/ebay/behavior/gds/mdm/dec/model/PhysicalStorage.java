package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.StorageContext;

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

import java.util.Set;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true, exclude = {"pipelines"})
@Table(name = "dec_physical_storage")
public class PhysicalStorage extends DecAuditable {

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type")
    private AccessType accessType;

    @DiffInclude
    @Column(name = "storage_details")
    private String storageDetails;

    @DiffInclude
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_environment")
    private PlatformEnvironment storageEnvironment = PlatformEnvironment.STAGING;

    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_context")
    private StorageContext storageContext;

    @DiffInclude
    @Column(name = "asset_id")
    private Long physicalAssetId;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "dec_physical_storage_pipeline_mapping",
            joinColumns = @JoinColumn(name = "storage_id", referencedColumnName = ID, insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "pipeline_id", referencedColumnName = ID, insertable = false, updatable = false)
    )
    private Set<Pipeline> pipelines;
}
