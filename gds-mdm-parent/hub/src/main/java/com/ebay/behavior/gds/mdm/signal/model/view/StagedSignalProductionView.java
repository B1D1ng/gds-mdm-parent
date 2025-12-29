package com.ebay.behavior.gds.mdm.signal.model.view;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.signal.common.model.AbstractStagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.Signal;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.Set;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString(exclude = {"fields", "events", "fieldGroups"})
@EqualsAndHashCode(callSuper = true, exclude = {"fields", "events", "fieldGroups"})
@Entity
@Table(name = "staged_signal_production_view")
public class StagedSignalProductionView extends AbstractStagedSignal implements Signal, Metadata {

    @Transient
    @DiffInclude
    @JsonManagedReference
    private Set<StagedField> fields;

    @Column(name = "domain_readable_name")
    @NotBlank
    private String domainReadableName;
}
