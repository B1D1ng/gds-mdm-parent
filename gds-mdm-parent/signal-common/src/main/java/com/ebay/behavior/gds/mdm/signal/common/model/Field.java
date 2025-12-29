package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.JavaType;

import org.apache.avro.Schema;

import java.util.Set;

public interface Field extends Auditable {

    String EVENT_TYPES = "eventTypes";

    String getName();

    String getDescription();

    String getTag();

    JavaType getJavaType();

    Schema getAvroSchema();

    String getExpression();

    ExpressionType getExpressionType();

    Boolean getIsMandatory();

    String getEventTypesAsString();

    Set<? extends Attribute> getAttributes();
}
