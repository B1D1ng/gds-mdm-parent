package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

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
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true, exclude = {"unstagedContract"})
@Entity
@EqualsAndHashCode(callSuper = true, exclude = {"unstagedContract"})
@Table(name = "pipeline")
public class ContractPipeline extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "contract_id")
    private Long contractId;

    @NotNull
    @Positive
    @Column(name = "contract_version")
    private Integer contractVersion;

    @Column(name = "dls_pipeline_id")
    private String dlsPipelineId;

    @NotNull
    @Column(name = "environment")
    @Enumerated(EnumType.STRING)
    private Environment environment;

    @NotNull
    @Column(name = "deploy_scope")
    @Enumerated(EnumType.STRING)
    private DeployScope deployScope;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "contract_id", insertable = false, updatable = false),
            @JoinColumn(name = "contract_version", insertable = false, updatable = false)
    })
    private UnstagedContract unstagedContract;
}