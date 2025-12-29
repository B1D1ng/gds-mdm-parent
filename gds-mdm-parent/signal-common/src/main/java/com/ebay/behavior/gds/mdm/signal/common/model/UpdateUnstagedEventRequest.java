package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * Represents a request to update an UnstagedEvent.
 * We cannot use the UnstagedEvent itself since only a subset of the fields can be updated.
 */
@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UpdateUnstagedEventRequest extends AbstractModel implements Model {

    private String name;

    private String description;

    @PositiveOrZero
    private Integer fsmOrder;

    @PositiveOrZero
    private Integer cardinality;

    private String expression;

    private String githubRepositoryUrl;

    @Enumerated(EnumType.STRING)
    private ExpressionType expressionType;

    @JsonIgnore
    public UpdateUnstagedEventRequest withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public UpdateUnstagedEventRequest withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }
}