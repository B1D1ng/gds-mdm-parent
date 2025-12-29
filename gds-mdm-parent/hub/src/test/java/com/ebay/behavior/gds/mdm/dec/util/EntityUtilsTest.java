package com.ebay.behavior.gds.mdm.dec.util;

import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.model.udc.ConsumableDataset;
import com.ebay.behavior.gds.mdm.dec.model.udc.DataStoreTableDetail;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalDataModel;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Iterator;

import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmField;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.MIN_VERSION;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntityUtilsTest {

    @Test
    void mapToConsumableDataset() {
        ObjectMapper objectMapper = new ObjectMapper();
        var ldmId = 1L;
        var ldmVersion = 1;
        var namespaceId = 2L;
        var datasetId = 1L;
        Namespace namespace = TestModelUtils.namespace();
        namespace.setId(namespaceId);
        Dataset dataset = TestModelUtils.dataset(ldmId, ldmVersion, namespaceId);
        dataset.setId(datasetId);
        dataset.setConsumptionParameters("{\"type\": \"HIVE\", \"general\": {\"concurrency\": \"1\"}, \"hive\": {\"retention\": {\"period\": \"2592000s\"}, " +
                "\"queryLatency\": {\"threshold\": \"7200s\"}, \"dataLatency\": {\"threshold\": \"7200s\"}, " +
                "\"frequency\": {\"type\": \"FREQUENCY_TYPE_BATCH\", \"interval\": \"FREQUENCY_INTERVAL_HOURLY\", \"cron\": \"15 * * * *\", " +
                "\"firstRunTime\": \"2025-04-08T07:40:00Z\", \"generateInstance\": \"GENERATE_INSTANCE_SCHEDULED_TIME\", \"scheduleOn\": true } }, " +
                "\"accessType\": \"ACCESS_TYPE_HIVE\"}");
        dataset.setRuntimeConfigurations("config");
        DataStoreTableDetail storeTableDetail = DataStoreTableDetail.builder()
                .dataTableName("test_table")
                .dataStoreName("test_db")
                .dataStoreType("HIVE")
                .build();

        ConsumableDataset consumableDataset = EntityUtils.mapToConsumableDataset(objectMapper, dataset, namespace, ldmId, "ldm_name",
                AccessType.HIVE, storeTableDetail, "test_pipeline");
        assertThat(consumableDataset.getConsumableDatasetId()).isEqualTo(datasetId);
        assertThat(consumableDataset.getLdmId()).isEqualTo(ldmId);
        assertThat(consumableDataset.getAccessType()).isEqualTo(AccessType.HIVE);
        assertThat(consumableDataset.getStoreTableDetail().getDataTableName()).isEqualTo("test_table");
        assertThat(consumableDataset.getAccessDetails()).isNotEmpty();
        assertThat(consumableDataset.getAccessDetails().get(0).getAccessMode()).isEqualTo("HIVE");
        assertThat(consumableDataset.getAccessDetails().get(0).getEntityName()).isEqualTo("test_table");
        assertThat(consumableDataset.getAccessDetails().get(0).getLatency()).isEqualTo("7200s");
        assertThat(consumableDataset.getAccessDetails().get(0).getAccount()).isEqualTo("b_gdsdec");
        assertThat(consumableDataset.getAccessDetails().get(0).getFrequency()).isEqualTo("FREQUENCY_INTERVAL_HOURLY");
        assertThat(consumableDataset.getAccessDetails().get(0).getRetention()).isEqualTo("2592000s");
        assertThat(consumableDataset.getAccessDetails().get(0).getPipelineId()).isEqualTo("test_pipeline");
        assertThat(consumableDataset.getAccessDetails().get(0).getBackendSystemParameters()).isEqualTo("config");
    }

    @Test
    void mapToConsumableDataset_EmptyConsumptionParameter() {
        ObjectMapper objectMapper = new ObjectMapper();
        var ldmId = 1L;
        var ldmVersion = 1;
        var namespaceId = 2L;
        var datasetId = 1L;
        Namespace namespace = TestModelUtils.namespace();
        namespace.setId(namespaceId);
        Dataset dataset = TestModelUtils.dataset(ldmId, ldmVersion, namespaceId);
        dataset.setId(datasetId);
        dataset.setConsumptionParameters(null);
        dataset.setRuntimeConfigurations("config");
        String ldmName = "ldm_name";
        String pipelineId = "test_pipeline";
        DataStoreTableDetail storeTableDetail = DataStoreTableDetail.builder()
                .dataTableName("test_table")
                .dataStoreName("test_db")
                .dataStoreType("HIVE")
                .build();

        ConsumableDataset consumableDataset = EntityUtils.mapToConsumableDataset(objectMapper, dataset, namespace, ldmId, ldmName, AccessType.HIVE, storeTableDetail, pipelineId);
        assertThat(consumableDataset.getConsumableDatasetId()).isEqualTo(datasetId);
        assertThat(consumableDataset.getLdmId()).isEqualTo(ldmId);
        assertThat(consumableDataset.getAccessDetails()).isNotEmpty();
        assertThat(consumableDataset.getAccessDetails().get(0).getAccessMode()).isEqualTo(null);
        assertThat(consumableDataset.getAccessDetails().get(0).getLatency()).isEqualTo(null);
        assertThat(consumableDataset.getAccessDetails().get(0).getFrequency()).isEqualTo(null);
        assertThat(consumableDataset.getAccessDetails().get(0).getRetention()).isEqualTo(null);

        dataset.setConsumptionParameters("{\"a\": \"1\"}");
        consumableDataset = EntityUtils.mapToConsumableDataset(objectMapper, dataset, namespace, ldmId, ldmName, AccessType.HIVE, storeTableDetail, pipelineId);
        assertThat(consumableDataset.getAccessDetails()).isNotEmpty();
        assertThat(consumableDataset.getAccessDetails().get(0).getAccessMode()).isEqualTo(null);
        assertThat(consumableDataset.getAccessDetails().get(0).getLatency()).isEqualTo(null);
        assertThat(consumableDataset.getAccessDetails().get(0).getFrequency()).isEqualTo(null);
        assertThat(consumableDataset.getAccessDetails().get(0).getRetention()).isEqualTo(null);

        dataset.setConsumptionParameters("{\"type\": \"HIVE\", \"hive\": \"1\"}");
        consumableDataset = EntityUtils.mapToConsumableDataset(objectMapper, dataset, namespace, ldmId, ldmName, AccessType.HIVE, storeTableDetail, pipelineId);
        assertThat(consumableDataset.getAccessDetails()).isNotEmpty();
        assertThat(consumableDataset.getAccessDetails().get(0).getAccessMode()).isEqualTo("HIVE");
        assertThat(consumableDataset.getAccessDetails().get(0).getLatency()).isEqualTo(null);
        assertThat(consumableDataset.getAccessDetails().get(0).getFrequency()).isEqualTo(null);
        assertThat(consumableDataset.getAccessDetails().get(0).getRetention()).isEqualTo(null);

        dataset.setConsumptionParameters("{\"type\": \"STREAM\", \"stream\": {\"retention\": {\"period\": \"86400s\"}, " +
                "\"dataLatency\": {\"threshold\": \"180s\"} }, \"accessType\": \"ACCESS_TYPE_TOPIC\"}");
        DataStoreTableDetail streamStoreTableDetail = DataStoreTableDetail.builder()
                .dataTableName("test_topic")
                .dataStoreName("test_stream")
                .dataStoreType("KAFKA")
                .build();
        consumableDataset = EntityUtils.mapToConsumableDataset(objectMapper, dataset, namespace, ldmId, ldmName, AccessType.STREAM, streamStoreTableDetail, pipelineId);
        assertThat(consumableDataset.getAccessDetails()).isNotEmpty();
        assertThat(consumableDataset.getAccessDetails().get(0).getAccessMode()).isEqualTo("STREAM");
        assertThat(consumableDataset.getAccessDetails().get(0).getLatency()).isEqualTo("180s");
        assertThat(consumableDataset.getAccessDetails().get(0).getRetention()).isEqualTo("86400s");

        dataset.setConsumptionParameters("{\"type\": \"API\", \"api\": {\"retention\": {\"period\": \"listing_end_time + 3*24*60*60\"}, " +
                "\"dataLatency\": {\"threshold\": \"180s\"} }, \"accessType\": \"ACCESS_TYPE_API\"}");
        DataStoreTableDetail apiStoreTableDetail = DataStoreTableDetail.builder()
                .dataTableName("test_api")
                .dataStoreName("test_api_store")
                .dataStoreType("API")
                .build();
        consumableDataset = EntityUtils.mapToConsumableDataset(objectMapper, dataset, namespace, ldmId, ldmName, AccessType.API, apiStoreTableDetail, pipelineId);
        assertThat(consumableDataset.getAccessDetails()).isNotEmpty();
        assertThat(consumableDataset.getAccessDetails().get(0).getAccessMode()).isEqualTo("API");
        assertThat(consumableDataset.getAccessDetails().get(0).getLatency()).isEqualTo("180s");
        assertThat(consumableDataset.getAccessDetails().get(0).getRetention()).isEqualTo("listing_end_time + 3*24*60*60");
    }

    @Test
    void mapToLogicalDataModel() {
        var ldmId = 1L;
        var namespaceId = 2L;
        LdmBaseEntity ldm = TestModelUtils.ldmBaseEntity(namespaceId);
        ldm.setId(ldmId);
        var field = ldmField(1L, MIN_VERSION);
        var viewUpdateDate = java.sql.Timestamp.valueOf(now());
        var viewUpdateUser = "update_user";
        
        // Set isDcs and dcsFields on the first view
        List<String> dcsFields = List.of("field1", "field2");
        var baseLdmRaw = LdmEntity.builder()
                .id(1L)
                .viewType(ViewType.RAW)
                .version(MIN_VERSION)
                .fields(Set.of(field))
                .updateDate(viewUpdateDate)
                .updateBy(viewUpdateUser)
                .isDcs(true)  // Set isDcs
                .dcsFields(dcsFields)  // Set dcsFields
                .build();
                
        var baseLdmSnapshot = LdmEntity.builder().id(2L).viewType(ViewType.SNAPSHOT).version(1).fields(Set.of(field)).build();
        ldm.setViews(new ArrayList<>(List.of(baseLdmRaw, baseLdmSnapshot)));
        Namespace namespace = Namespace.builder().id(namespaceId).name("ns").type(NamespaceType.BASE).build();

        LogicalDataModel model = EntityUtils.mapToLogicalDataModel(ldm, namespace);
        assertThat(model.getLogicalDataModelId()).isEqualTo(ldmId);
        assertThat(model.getType().name()).isEqualTo("BASE");
        assertThat(model.getViews().size()).isEqualTo(2);
        assertThat(model.getFields().size()).isEqualTo(1);
        assertThat(model.getFields().get(0).getViews().size()).isEqualTo(2);
        assertThat(model.getUpdateDate()).isEqualTo(viewUpdateDate);
        assertThat(model.getUpdateBy()).isEqualTo(viewUpdateUser);
        
        // Assert that isDcs and dcsFields are correctly mapped from the first view
        assertThat(model.getIsDcs()).isEqualTo(true);
        assertThat(model.getDcsFields()).isEqualTo(dcsFields);
    }

    @Test
    void getStorageDetail() {
        ObjectMapper objectMapper = new ObjectMapper();
        PhysicalStorage storage = PhysicalStorage.builder().accessType(AccessType.HIVE).storageDetails("{\"table_name\": \"abc\"}").build();
        DataStoreTableDetail detail = EntityUtils.getStorageDetail(objectMapper, storage);
        assertThat(detail).isNotNull();
        assertThat(detail.getDataTableName()).isEqualTo("abc");
        assertThat(detail.getDataStoreName()).isEqualTo("APOLLO_RNO.GDS_T");
        assertThat(detail.getDataStoreType()).isEqualTo("Hive");

        storage = PhysicalStorage.builder().accessType(AccessType.STREAM).storageDetails("{\"topicName\": \"topic\", \"streamName\": \"stream\"}").build();
        detail = EntityUtils.getStorageDetail(objectMapper, storage);
        assertThat(detail).isNotNull();
        assertThat(detail.getDataTableName()).isEqualTo("topic");
        assertThat(detail.getDataStoreName()).isEqualTo("stream");
        assertThat(detail.getDataStoreType()).isEqualTo("Kafka");

        storage = PhysicalStorage.builder().accessType(AccessType.API).storageDetails("{\"read_url\": \"http\"}").build();
        detail = EntityUtils.getStorageDetail(objectMapper, storage);
        assertThat(detail).isNull();

        AccessType mockAccessType = mock(AccessType.class);
        when(mockAccessType.name()).thenReturn("mock");
        storage.setAccessType(mockAccessType);
        detail = EntityUtils.getStorageDetail(objectMapper, storage);
        assertThat(detail).isNull();

        storage.setStorageDetails(null);
        detail = EntityUtils.getStorageDetail(objectMapper, storage);
        assertThat(detail).isNull();
    }

    @Test
    void getLdmName() {
        // entityName is null
        assertThat(EntityUtils.getLdmName(null, ViewType.NONE)).isNull();

        // entityName is empty
        assertThat(EntityUtils.getLdmName("", ViewType.NONE)).isEmpty();

        // viewType is null
        assertThat(EntityUtils.getLdmName("Entity", null)).isEqualTo("entity");

        // viewType is NONE
        assertThat(EntityUtils.getLdmName("Entity", ViewType.NONE)).isEqualTo("entity");

        // viewType is not NONE
        assertThat(EntityUtils.getLdmName("Entity", ViewType.RAW)).isEqualTo("entity_raw");
    }

    @Test
    void copyLdm_fieldsNull() {
        LdmEntity source = new LdmEntity();
        source.setFields(null);
        EntityManager em = mock(EntityManager.class);
        LdmEntity result = EntityUtils.copyLdm(source, em, "user1");
        assertThat(result).isNotNull();
        assertThat(result.getFields()).isNull();
        assertThat(result.getUpdateBy()).isEqualTo("user1");
    }

    @Test
    void copyLdm_fieldsNoMappings() {
        LdmEntity source = TestModelUtils.ldmEntityEmpty(1L);
        var field = TestModelUtils.ldmFieldEmpty();
        field.setSignalMapping(null);
        field.setPhysicalStorageMapping(null);
        source.setFields(Set.of(field));

        EntityManager em = mock(EntityManager.class);
        LdmEntity result = EntityUtils.copyLdm(source, em, "user2");
        assertThat(result).isNotNull();
        assertThat(result.getFields().size()).isEqualTo(1);
        assertThat(result.getUpdateBy()).isEqualTo("user2");
    }

    @Test
    void copyLdm_fieldsWithMappings() {
        LdmEntity source = TestModelUtils.ldmEntityWithFieldsAndSignalMapping(1L);
        EntityManager em = mock(EntityManager.class);
        LdmEntity result = EntityUtils.copyLdm(source, em, "user3");
        assertThat(result).isNotNull();
        assertThat(result.getFields().size()).isEqualTo(2);
        var itemIdField = result.getFields().stream().filter(f -> "item_id".equalsIgnoreCase(f.getName())).findFirst();
        var itemTitleField = result.getFields().stream().filter(f -> "auct_title".equalsIgnoreCase(f.getName())).findFirst();
        assertThat(itemIdField).isPresent();
        assertThat(itemTitleField).isPresent();
        assertThat(itemIdField.get().getSignalMapping().size()).isEqualTo(2);
        assertThat(itemTitleField.get().getSignalMapping()).isNull();
        assertThat(result.getUpdateBy()).isEqualTo("user3");
    }

    @Test
    @SuppressWarnings("PMD.LooseCoupling")
    void getSortedFieldsByOrdinal() {
        // Test case 1: Fields with different ordinals
        var field1 = ldmField(1L, MIN_VERSION);
        var field2 = ldmField(2L, MIN_VERSION);
        var field3 = ldmField(3L, MIN_VERSION);
        
        field1.setOrdinal(3);
        field2.setOrdinal(1);
        field3.setOrdinal(2);
        
        Set<LdmField> fields = Set.of(field1, field2, field3);
        Set<LdmField> sortedFields = EntityUtils.getSortedFieldsByOrdinal(fields);
        
        // Verify the result maintains order (we're intentionally checking the implementation)
        @SuppressWarnings("PMD.LooseCoupling")
        boolean isOrderPreservingSet = sortedFields instanceof LinkedHashSet;
        assertThat(isOrderPreservingSet).isTrue();
        
        // Verify the order of fields based on ordinal
        Iterator<LdmField> iterator = sortedFields.iterator();
        assertThat(iterator.next().getOrdinal()).isEqualTo(1);
        assertThat(iterator.next().getOrdinal()).isEqualTo(2);
        assertThat(iterator.next().getOrdinal()).isEqualTo(3);

        // Test case 2: Fields with some null ordinals
        var fieldWithNull1 = ldmField(4L, MIN_VERSION);
        var fieldWithNull2 = ldmField(5L, MIN_VERSION);
        
        fieldWithNull1.setOrdinal(null);
        fieldWithNull2.setOrdinal(5);
        
        Set<LdmField> fieldsWithNull = Set.of(fieldWithNull1, fieldWithNull2);
        Set<LdmField> sortedFieldsWithNull = EntityUtils.getSortedFieldsByOrdinal(fieldsWithNull);
        
        // Verify null ordinals are placed last
        iterator = sortedFieldsWithNull.iterator();
        assertThat(iterator.next().getOrdinal()).isEqualTo(5);
        assertThat(iterator.next().getOrdinal()).isNull();

        // Test case 3: Empty set
        Set<LdmField> emptySet = Set.of();
        Set<LdmField> sortedEmptySet = EntityUtils.getSortedFieldsByOrdinal(emptySet);
        assertThat(sortedEmptySet).isEmpty();

        // Test case 4: Null input
        Set<LdmField> sortedNull = EntityUtils.getSortedFieldsByOrdinal(null);
        assertThat(sortedNull).isEmpty();
    }

    @Test
    void copyBasicInfoFromBaseEntity_copiesFieldsCorrectly() {
        val baseEntity = new LdmBaseEntity();
        baseEntity.setPk("pk123");
        baseEntity.setOwners("owner1");
        baseEntity.setJiraProject("JIRA-1");
        baseEntity.setTeam("TeamA");
        baseEntity.setTeamDl("teamA@company.com");

        val ldmEntity = new LdmEntity();

        EntityUtils.copyBasicInfoFromBaseEntity(ldmEntity, baseEntity);

        assertEquals("pk123", ldmEntity.getPk());
        assertEquals("owner1", ldmEntity.getOwners());
        assertEquals("JIRA-1", ldmEntity.getJiraProject());
        assertEquals("TeamA", ldmEntity.getTeam());
        assertEquals("teamA@company.com", ldmEntity.getTeamDl());
    }
    
    @Test
    void excludeTextFields_removesTextFields() {
        // Create 
        LdmEntity entity = TestModelUtils.ldmEntityEmpty(1L);
        entity.setIr("ir123");
        entity.setCodeContent("code content");
        entity.setGeneratedSql("generatedSql"); 

        // Exclude text fields
        EntityUtils.excludeTextFields(entity);

        // Verify the result
        assertThat(entity.getIr()).isNull();
        assertThat(entity.getCodeContent()).isNull();
        assertThat(entity.getGeneratedSql()).isNull();
    }
}
