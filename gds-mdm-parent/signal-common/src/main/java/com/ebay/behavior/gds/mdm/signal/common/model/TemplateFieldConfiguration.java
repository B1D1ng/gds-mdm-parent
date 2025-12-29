package com.ebay.behavior.gds.mdm.signal.common.model;

import lombok.Data;

import java.util.List;

/**
 * Root configuration model for template fields from JSON
 * Generic class that can be used for any template creation process
 * Represents a list of template field definitions directly (without wrapper object)
 */
@Data
public class TemplateFieldConfiguration {

    private List<TemplateFieldDefinition> fields;

    public TemplateFieldConfiguration(List<TemplateFieldDefinition> fields) {
        this.fields = fields;
    }

    public TemplateFieldConfiguration() {
    }
}