package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.dec.model.enums.DatasetStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true, exclude = {"dataset"})
@Table(name = "dec_dataset_deployment")
public class DatasetDeployment extends AbstractModel {

    @NotNull
    @Column(name = "dataset_id")
    private Long datasetId;

    @NotNull
    @Column(name = "dataset_version")
    private Integer datasetVersion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private PlatformEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DatasetStatus status;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({@JoinColumn(name = "dataset_id", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "dataset_version", referencedColumnName = "version", insertable = false, updatable = false)})
    private Dataset dataset;
}
