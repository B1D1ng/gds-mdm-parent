package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.Model;

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
public class UnstagedFieldProxy extends AbstractModel implements Model {

    private String name;
    private String description;
    private String expression;
    private ExpressionType expressionType;
    private Boolean isMandatory;
}