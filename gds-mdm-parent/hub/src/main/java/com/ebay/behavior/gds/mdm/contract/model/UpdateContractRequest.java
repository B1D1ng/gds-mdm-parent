package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UpdateContractRequest extends AbstractModel implements Model {

    private String name;

    private String owners;

    private String entityType;

    private String domain;

    private String dl;

    private String description;

    private ContractStatus status;

    private Environment environment;

    @JsonIgnore
    public UpdateContractRequest withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public UpdateContractRequest withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }
}