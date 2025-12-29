package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * Represents a request to update an UnstagedAttribute.
 * We cannot use the UnstagedAttribute itself since only a subset of the properties can be updated.
 */
@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UpdateUnstagedAttributeRequest extends AbstractModel implements Model {

    private String description;

    @Enumerated(EnumType.STRING)
    private JavaType javaType;

    private String schemaPath;

    private Boolean isStoreInState;

    @JsonIgnore
    public UpdateUnstagedAttributeRequest withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public UpdateUnstagedAttributeRequest withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }
}