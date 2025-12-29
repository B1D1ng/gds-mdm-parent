package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.dec.model.enums.SignalType;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffInclude;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dec_ldm_field_signal_mapping")
public class LdmFieldSignalMapping extends DecAuditable {

    @Column(name = "ldm_field_id")
    private Long ldmFieldId;

    @DiffInclude
    @NotNull
    @Column(name = "signal_def_id")
    private Long signalDefinitionId;

    @DiffInclude
    @NotNull
    @Column(name = "signal_version")
    private Integer signalVersion;

    @DiffInclude
    @Column(name = "signal_name")
    private String signalName;

    @DiffInclude
    @Column(name = "signal_type")
    private SignalType signalType;

    @DiffInclude
    @NotNull
    @Column(name = "signal_field_name")
    private String signalFieldName;

    @DiffInclude
    @Column(name = "signal_field_expression")
    private String signalFieldExpression;

    @DiffInclude
    @Column(name = "signal_field_expression_online")
    private String signalFieldExpressionOnline;

    @DiffInclude
    @Column(name = "signal_field_expression_offline")
    private String signalFieldExpressionOffline;

    @DiffInclude
    @Column(name = "signal_field_latency")
    private String signalFieldLatency;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ldm_field_id", insertable = false, updatable = false)
    private LdmField field;
}
