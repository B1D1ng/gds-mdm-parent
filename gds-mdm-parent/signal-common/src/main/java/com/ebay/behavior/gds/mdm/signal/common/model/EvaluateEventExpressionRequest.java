package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.ExpressionType;

public record EvaluateEventExpressionRequest(String expression, ExpressionType expressionType) {
}