package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.common.model.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * Represents a request to update an UnstagedSignal.
 * We cannot use the UnstagedSignal itself since only a subset of the fields can be updated.
 */
@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UpdateUnstagedSignalRequest extends AbstractModel implements Model {

    private String name;

    private String description;

    private Long retentionPeriod;

    private String uuidGeneratorType;

    private String uuidGeneratorExpression;

    @JsonIgnore
    public UpdateUnstagedSignalRequest withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public UpdateUnstagedSignalRequest withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }
}