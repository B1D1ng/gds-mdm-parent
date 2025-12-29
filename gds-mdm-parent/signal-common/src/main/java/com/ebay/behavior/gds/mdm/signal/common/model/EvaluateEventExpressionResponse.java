package com.ebay.behavior.gds.mdm.signal.common.model;

import java.util.List;

public record EvaluateEventExpressionResponse(List<FieldGroup<UnstagedField>> currentFields, List<FieldGroup<UnstagedField>> nextFields) {
}
