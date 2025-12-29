package com.ebay.behavior.gds.mdm.signal.common.model;

import java.util.Set;

public record CreateFieldTemplateRequest(FieldTemplate field, Set<Long> attributeIds, Set<Long> eventTypeIds) {
}
