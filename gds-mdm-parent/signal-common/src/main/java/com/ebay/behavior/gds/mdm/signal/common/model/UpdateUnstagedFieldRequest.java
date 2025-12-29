package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Set;

/**
 * Represents a request to update an UnstagedField.
 * We cannot use the UnstagedField itself since only a subset of the properties can be updated.
 */
@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UpdateUnstagedFieldRequest extends UnstagedFieldProxy implements Model {
    private Set<Long> attributeIds;
}