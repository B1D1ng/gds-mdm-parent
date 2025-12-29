package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.dec.model.enums.DeploymentStatus;

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
import lombok.ToString;
import org.javers.core.metamodel.annotation.DiffInclude;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true)
@Table(name = "dec_dataset_physical_storage_mapping")
public class DatasetPhysicalStorageMapping extends DecAuditable {

    @NotNull
    @Column(name = "dataset_id")
    private Long datasetId;

    @NotNull
    @Column(name = "dataset_version")
    private Integer datasetVersion;

    @NotNull
    @DiffInclude
    @Column(name = "physical_storage_id")
    private Long physicalStorageId;

    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeploymentStatus status;
}
