package com.ebay.behavior.gds.mdm.signal.common.model;

import java.util.List;

/**
 * Record representing the enriched signal template result containing created attributes and fields.
 * This record encapsulates the outcome of enriching a signal template with additional
 * attributes and field templates.
 *
 * @param attributeIds List of created attribute template IDs
 * @param fields List of created field templates
 */
public record TemplateEnrichedRecord(
    List<Long> attributeIds,
    List<FieldTemplate> fields
) {}