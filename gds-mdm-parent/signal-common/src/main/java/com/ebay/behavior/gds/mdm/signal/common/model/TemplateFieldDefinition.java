package com.ebay.behavior.gds.mdm.signal.common.model;

import lombok.Data;

/**
 * Model representing a template field definition from JSON configuration
 * Generic class that can be used for any template creation process
 */
@Data
public class TemplateFieldDefinition {

    private String attributeName;
    private String fieldName;
    private String description;
    private String javaType;
    private String schemaPath;
    private boolean mandatory;
    private String avroSchema;
}