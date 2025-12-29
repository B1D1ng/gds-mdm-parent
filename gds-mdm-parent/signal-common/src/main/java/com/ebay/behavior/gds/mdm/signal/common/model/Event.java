package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;

import java.util.Set;

public interface Event extends Auditable {

    String getName();

    String getDescription();

    String getType();

    EventSource getSource();

    Integer getFsmOrder();

    Integer getCardinality();

    String getExpression();

    ExpressionType getExpressionType();

    Set<? extends Attribute> getAttributes();
}
