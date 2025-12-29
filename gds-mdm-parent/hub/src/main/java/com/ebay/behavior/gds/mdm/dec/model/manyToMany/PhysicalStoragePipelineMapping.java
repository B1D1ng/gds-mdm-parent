package com.ebay.behavior.gds.mdm.dec.model.manyToMany;

import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Entity
@ToString
@EqualsAndHashCode
@Table(name = "dec_physical_storage_pipeline_mapping")
public class PhysicalStoragePipelineMapping {

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "storage_id", referencedColumnName = ID)
    private PhysicalStorage physicalStorage;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "pipeline_id", referencedColumnName = ID)
    private Pipeline pipeline;

    public PhysicalStoragePipelineMapping(PhysicalStorage physicalStorage, Pipeline pipeline) {
        this.pipeline = pipeline;
        this.physicalStorage = physicalStorage;
    }
}
