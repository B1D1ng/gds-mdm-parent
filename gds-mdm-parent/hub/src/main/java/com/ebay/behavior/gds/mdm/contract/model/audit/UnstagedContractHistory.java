package com.ebay.behavior.gds.mdm.contract.model.audit;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.audit.AbstractVersionedHistoryAuditable;
import com.ebay.behavior.gds.mdm.contract.model.ContractStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "contract_history")
public class UnstagedContractHistory extends AbstractVersionedHistoryAuditable {
    @NotBlank
    @DiffInclude
    @Column(name = NAME)
    private String name;

    @NotBlank
    @DiffInclude
    @Column(name = "owners")
    private String owners;

    @NotBlank
    @DiffInclude
    @Column(name = "entityType")
    private String entityType;

    @NotBlank
    @DiffInclude
    @Column(name = "dl")
    private String dl;

    @NotBlank
    @DiffInclude
    @Column(name = "description")
    private String description;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ContractStatus status;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment;
}
