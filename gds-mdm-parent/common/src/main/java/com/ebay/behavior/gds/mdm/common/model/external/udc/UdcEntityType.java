package com.ebay.behavior.gds.mdm.common.model.external.udc;

import com.ebay.behavior.gds.mdm.common.model.Metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UdcEntityType {
    // generic types
    TRANSFORMATION("Transformation", "TransformationId"),
    COLUMN_TRANSFORMATION("ColumnTransformation", "ColumnTransformationId"),

    // custom types
    SIGNAL("Signal", "SignalId"),
    FIELD("SignalField", "SignalFieldId"),
    EVENT("UnifiedEvent", "unifiedEventId"),
    ATTRIBUTE("UnifiedEventAttribute", "unifiedEventAttributeId"),
    LDM("LogicalDataModel", "logicalDataModelId"),
    LDM_FIELD("LogicalField", "logicalFieldId"),
    DATASET("ConsumableDataset", "consumableDatasetId"),
    UDF("UDF", "udfId"),
    UDF_STUB("UdfStub", "udfStubId"),
    DATA_TABLE("DataTable", "dataTableId"),
    RHEOS_KAFKA_TOPIC("RheosKafkaTopic", "rheosKafkaTopicId");

    private final String value;
    private final String idName;

    UdcEntityType(String value, String idName) {
        this.value = value;
        this.idName = idName;
    }

    @JsonCreator
    public static UdcEntityType fromValue(String text) {
        for (UdcEntityType b : UdcEntityType.values()) {
            if (String.valueOf(b.value).equals(String.valueOf(text))) {
                return b;
            }
        }
        throw new IllegalArgumentException(String.format("Unexpected value: %s", text));
    }

    public static UdcEntityType fromType(Class<? extends Metadata> type) {
        return switch (type.getSimpleName()) {
            case "UnstagedSignal", "StagedSignal" -> SIGNAL;
            case "UnstagedField", "StagedField" -> FIELD;
            case "UnstagedEvent", "StagedEvent" -> EVENT;
            case "UnstagedAttribute", "StagedAttribute" -> ATTRIBUTE;
            case "LogicalDataModel" -> LDM;
            case "LogicalField" -> LDM_FIELD;
            case "ConsumableDataset" -> DATASET;
            case "UnstagedUdf", "StagedUdf" -> UDF;
            case "UnstagedUdfStub", "StagedUdfStub" -> UDF_STUB;
            default -> throw new IllegalArgumentException(String.format("Unexpected type: %s", type.getSimpleName()));
        };
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getIdName() {
        return idName;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
