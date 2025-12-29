package com.ebay.behavior.gds.mdm.dec.testUtil;

import com.ebay.behavior.gds.mdm.dec.model.*;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmChangeRequestLogRecord;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmEntityRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.*;
import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalStoragePipelineMapping;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.toNowSqlTimestamp;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomString;

@UtilityClass
public class TestModelUtils {

    public static final String IT_TEST_USER = "IT_test_user";

    public static Namespace namespace() {
        return Namespace.builder()
                .name(getRandomString())
                .type(NamespaceType.DOMAIN)
                .owners(IT_TEST_USER)
                .build();
    }

    public static Dataset dataset(Long id, Integer version, Long ldmEntityId, Integer ldmVersion) {
        return Dataset.builder()
                .id(id)
                .version(version)
                .name(getRandomString())
                .ldmEntityId(ldmEntityId)
                .ldmVersion(ldmVersion)
                .status(DatasetStatus.DRAFT)
                .consumptionParameters("{\"hive_table\": \"table_xyz\", \"hadoop_queue\":\"\", \"retention\":\"\", \"offline_schedule\":\"\"}")
                .runtimeConfigurations("{\"spark_configs\": \"\"}")
                .build();
    }

    public static Dataset dataset(Long ldmEntityId, Integer ldmVersion, Long namespaceId) {
        return Dataset.builder()
                .name(getRandomString())
                .ldmEntityId(ldmEntityId)
                .ldmVersion(ldmVersion)
                .namespaceId(namespaceId)
                .status(DatasetStatus.DRAFT)
                .consumptionParameters("{\"hive_table\": \"table_xyz\", \"hadoop_queue\":\"\", \"retention\":\"\", \"offline_schedule\":\"\"}")
                .runtimeConfigurations("{\"spark_configs\": \"\"}")
                .build();
    }

    public static DatasetIndex datasetIndex() {
        return DatasetIndex.builder()
                .name(getRandomString())
                .currentVersion(1)
                .build();
    }

    public static LdmBaseEntity ldmBaseEntity(Long namespaceId) {
        return LdmBaseEntity.builder()
                .name(getRandomString())
                .namespaceId(namespaceId)
                .build();
    }

    public static LdmEntityIndex ldmEntityIndex(Long baseEntityId) {
        return LdmEntityIndex.builder()
                .viewType(ViewType.RAW)
                .baseEntityId(baseEntityId)
                .currentVersion(1)
                .build();
    }

    public static LdmEntity ldmEntity(Long id, Integer version, String name, ViewType viewType, Long namespaceId, Long baseEntityId) {
        return LdmEntity.builder()
                .id(id)
                .version(version)
                .name(name)
                .viewType(viewType)
                .description(getRandomString())
                .owners(IT_TEST_USER)
                .jiraProject("GDS")
                .domain("GDS")
                .pk("ITEM_ID")
                .namespaceId(namespaceId)
                .baseEntityId(baseEntityId)
                .build();
    }

    public static LdmEntity ldmEntityEmpty(Long namespaceId) {
        return LdmEntity.builder()
                .name(getRandomString())
                .viewType(ViewType.RAW)
                .description(getRandomString())
                .owners(IT_TEST_USER)
                .jiraProject("GDS")
                .domain("GDS")
                .pk("ITEM_ID")
                .namespaceId(namespaceId)
                .build();
    }

    public static LdmEntityRequest ldmEntityRequest(Long namespaceId) {
        return LdmEntityRequest.builder()
                .name(getRandomString())
                .viewType(ViewType.RAW)
                .description(getRandomString())
                .owners(IT_TEST_USER)
                .jiraProject("GDS")
                .domain("GDS")
                .pk("ITEM_ID")
                .namespaceId(namespaceId)
                .build();
    }

    public static LdmEntity derivedLdm(Long namespaceId, LdmBaseEntity baseEntity) {
        return LdmEntity.builder()
                .name(baseEntity.getName())
                .viewType(ViewType.NONE)
                .description(getRandomString())
                .owners(baseEntity.getOwners())
                .jiraProject(baseEntity.getJiraProject())
                .domain(baseEntity.getDomain())
                .pk(baseEntity.getPk())
                .namespaceId(namespaceId)
                .baseEntityId(baseEntity.getId())
                .build();
    }

    public static LdmEntity ldmEntityEmptyWithBaseEntity(Long namespaceId, Long baseEntityId) {
        return LdmEntity.builder()
                .name(getRandomString())
                .viewType(ViewType.RAW)
                .description(getRandomString())
                .owners(IT_TEST_USER)
                .jiraProject("GDS")
                .domain("GDS")
                .pk("ITEM_ID")
                .namespaceId(namespaceId)
                .baseEntityId(baseEntityId)
                .build();
    }

    public static LdmEntity ldmEntityWithFieldsAndSignalMapping(Long namespaceId) {
        var entity = ldmEntityEmpty(namespaceId);
        LdmField field1 = LdmField.builder().name("item_id").description("item id").dataType("decimal(38,0)").build();
        LdmFieldSignalMapping fieldSignalMapping1 = LdmFieldSignalMapping.builder()
                .signalDefinitionId(1L).signalVersion(1).signalFieldName("field1").build();
        LdmFieldSignalMapping fieldSignalMapping2 = LdmFieldSignalMapping.builder()
                .signalDefinitionId(2L).signalVersion(1).signalFieldName("field1").build();
        field1.setSignalMapping(Set.of(fieldSignalMapping1, fieldSignalMapping2));
        LdmField field2 = LdmField.builder().name("auct_title").description("item title").dataType("string").build();
        entity.setFields(Set.of(field1, field2));
        return entity;
    }

    public static LdmField ldmFieldEmpty() {
        return LdmField.builder()
                .name(getRandomString())
                .description(getRandomString())
                .dataType("decimal(38,0)")
                .signalFilter(List.of("Item_New", "Item_Revise"))
                .build();
    }

    public static LdmChangeRequestLogRecord ldmChangeRequestLogEntry() {
        return LdmChangeRequestLogRecord.builder()
                .userName(IT_TEST_USER)
                .status(ChangeRequestStatus.APPROVED)
                .createdTime(toNowSqlTimestamp())
                .build();
    }

    public static LdmChangeRequestLogRecord ldmChangeRequestLogEntryReject() {
        return LdmChangeRequestLogRecord.builder()
                .userName(IT_TEST_USER)
                .status(ChangeRequestStatus.REJECTED)
                .comment("Reject Reason")
                .createdTime(toNowSqlTimestamp())
                .build();
    }

    public static LdmChangeRequest ldmViewCreateChangeRequest(Long baseEntityId) {
        return LdmChangeRequest.builder()
                .actionType(ActionType.CREATE)
                .actionTarget(ActionTarget.LDM_VIEW)
                .requestDetails("{\"baseEntityId\": " + baseEntityId + ", \"viewType\": \"RAW\", "
                        + "\"fields\": [ { \"name\": \"item_id\", "
                        + "\"description\": \"item id\", "
                        + "\"dataType\": \"decimal(38,0)\", \"valueFunction\": null, "
                        + "\"signalMapping\": [ { \"signalDefinitionId\": \"5355869\", \"signalVersion\": \"1\", "
                        + "\"signalFieldName\": \"signal.itemContext.basic.id\", \"signalFieldExpressionOnline\": null, "
                        + "\"signalFieldExpressionOffline\": \"signal.itemContext.basic.id\" }, "
                        + "{ \"signalDefinitionId\": \"7208421\", \"signalVersion\": \"1\", "
                        + "\"signalFieldName\": \"signal.itemContext.basic.id\", \"signalFieldExpressionOnline\": null, "
                        + "\"signalFieldExpressionOffline\": \"signal.itemContext.basic.id\" } ] }, "
                        + "{ \"name\": \"auct_title\", \"description\": \"item title\", \"dataType\": \"string\", \"valueFunction\": null, "
                        + "\"signalMapping\": [ { \"signalDefinitionId\": \"5355869\", \"signalVersion\": \"1\", "
                        + "\"signalFieldName\": \"signal.itemContext.basic.title\", \"signalFieldExpressionOnline\": null, "
                        + "\"signalFieldExpressionOffline\": \"signal.itemContext.basic.title\" } ] }, "
                        + "{ \"name\": \"gallery_guid\", \"description\": \"gallery_guid\", \"dataType\": \"string\", \"valueFunction\": null, "
                        + "\"signalMapping\": [ { \"signalDefinitionId\": \"5355869\", \"signalVersion\": \"1\", "
                        + "\"signalFieldName\": \"signal.itemContext.basic.galleryGuid\", \"signalFieldExpressionOnline\": null, "
                        + "\"signalFieldExpressionOffline\": \"signal.itemContext.basic.galleryGuid\" } ] } ] }")
                .build();
    }

    public static LdmChangeRequest ldmChangeRequest(Long entityId) {
        return LdmChangeRequest.builder()
                .actionType(ActionType.UPDATE)
                .actionTarget(ActionTarget.LDM_VIEW)
                .requestDetails("{\"id\": " + entityId + ", \"fields\": [ { \"name\": \"item_id\", \"description\": \"item id\", "
                        + "\"dataType\": \"decimal(38,0)\", \"valueFunction\": null, "
                        + "\"signalMapping\": [ { \"signalDefinitionId\": \"5355869\", \"signalVersion\": \"1\", "
                        + "\"signalFieldName\": \"signal.itemContext.basic.id\", \"signalFieldExpressionOnline\": null, "
                        + "\"signalFieldExpressionOffline\": \"signal.itemContext.basic.id\" }, "
                        + "{ \"signalDefinitionId\": \"7208421\", \"signalVersion\": \"1\", "
                        + "\"signalFieldName\": \"signal.itemContext.basic.id\", \"signalFieldExpressionOnline\": null, "
                        + "\"signalFieldExpressionOffline\": \"signal.itemContext.basic.id\" } ] }, "
                        + "{ \"name\": \"auct_title\", \"description\": \"item title\", \"dataType\": \"string\", \"valueFunction\": null, "
                        + "\"signalMapping\": [ { \"signalDefinitionId\": \"5355869\", \"signalVersion\": \"1\", "
                        + "\"signalFieldName\": \"signal.itemContext.basic.title\", \"signalFieldExpressionOnline\": null, "
                        + "\"signalFieldExpressionOffline\": \"signal.itemContext.basic.title\" } ] }, "
                        + "{ \"name\": \"gallery_guid\", \"description\": \"gallery_guid\", \"dataType\": \"string\", \"valueFunction\": null, "
                        + "\"signalMapping\": [ { \"signalDefinitionId\": \"5355869\", \"signalVersion\": \"1\", "
                        + "\"signalFieldName\": \"signal.itemContext.basic.galleryGuid\", \"signalFieldExpressionOnline\": null, "
                        + "\"signalFieldExpressionOffline\": \"signal.itemContext.basic.galleryGuid\" } ] } ] }")
                .build();
    }

    public static LdmChangeRequest ldmChangeRequest(Long entityId, Long baseEntityId, ViewType viewType) {
        return LdmChangeRequest.builder()
                .actionType(ActionType.UPDATE)
                .actionTarget(ActionTarget.LDM_VIEW)
                .requestDetails("{\"id\": " + entityId + ", \"baseEntityId\": " + baseEntityId + ", \"viewType\": \"" + viewType + "\"}")
                .build();
    }

    public static LdmChangeRequest namespaceChangeRequest() {
        return LdmChangeRequest.builder()
                .actionType(ActionType.CREATE)
                .actionTarget(ActionTarget.NAMESPACE)
                .requestDetails("{\"name\": \"Test Namespace\", \"type\": \"BASE\", \"owners\": \"owner list\"}")
                .build();
    }

    public static PhysicalAsset physicalAsset() {
        return PhysicalAsset.builder()
                .assetType(PhysicalAssetType.NUKV)
                .assetName(getRandomString())
                .build();
    }

    public static PhysicalStorage physicalStorage() {
        return PhysicalStorage.builder()
                .accessType(AccessType.HIVE)
                .storageDetails(
                        String.format(
                                "{\"table_name\": \"%s\", \"hadoop_queue\":\"default\", \"retention\":\"30\", \"offline_schedule\":\"0 2 * * *\"}",
                                getRandomString()))
                .build();
    }

    public static PhysicalStoragePipelineMapping physicalStoragePipelineMapping(PhysicalStorage physicalStorage, Pipeline pipeline) {
        return PhysicalStoragePipelineMapping.builder()
                .pipeline(pipeline)
                .physicalStorage(physicalStorage)
                .build();
    }

    public static Pipeline pipeline() {
        return Pipeline.builder()
                .pipelineId(getRandomString())
                .name(getRandomString())
                .build();
    }

    public static DatasetPhysicalStorageMapping datasetPhysicalStorageMapping(Long datasetId, Integer datasetVersion, Long physicalStorageId) {
        return DatasetPhysicalStorageMapping.builder()
                .datasetId(datasetId)
                .datasetVersion(datasetVersion)
                .physicalStorageId(physicalStorageId)
                .build();
    }

    public static LdmField ldmField(Long entityId, Integer ldmVersion) {
        return LdmField.builder()
                .name(getRandomString())
                .description(getRandomString())
                .dataType("decimal(38,0)")
                .ldmEntityId(entityId)
                .ldmVersion(ldmVersion)
                .build();
    }

    public static LdmFieldSignalMapping ldmFieldSignalMapping(Long ldmFieldId) {
        return LdmFieldSignalMapping.builder()
                .signalDefinitionId(1L)
                .signalVersion(1)
                .signalFieldName(getRandomString())
                .ldmFieldId(ldmFieldId)
                .build();
    }

    public static LdmFieldSignalMapping ldmFieldSignalMappingEmpty() {
        return LdmFieldSignalMapping.builder()
                .signalDefinitionId(1L)
                .signalVersion(1)
                .signalFieldName(getRandomString())
                .build();
    }

    public static LdmFieldPhysicalStorageMapping ldmFieldPhysicalMapping(Long ldmFieldId, Long physicalStorageId) {
        return LdmFieldPhysicalStorageMapping.builder()
                .ldmFieldId(ldmFieldId)
                .physicalStorageId(physicalStorageId)
                .physicalFieldExpression(getRandomSmallString())
                .build();
    }

    public static LdmFieldPhysicalStorageMapping ldmFieldPhysicalMappingEmpty(Long physicalStorageId) {
        return LdmFieldPhysicalStorageMapping.builder()
                .physicalStorageId(physicalStorageId)
                .build();
    }

    public static LdmErrorHandlingStorageMapping ldmErrorHandlingStorageMapping(Long ldmEntityId, Integer ldmVersion, Long physicalStorageId) {
        return LdmErrorHandlingStorageMapping.builder()
                .ldmEntityId(ldmEntityId)
                .ldmVersion(ldmVersion)
                .physicalStorageId(physicalStorageId)
                .build();
    }

    public static LdmErrorHandlingStorageMapping ldmErrorHandlingStorageMappingEmpty(Long physicalStorageId) {
        return LdmErrorHandlingStorageMapping.builder()
                .physicalStorageId(physicalStorageId)
                .build();
    }
}