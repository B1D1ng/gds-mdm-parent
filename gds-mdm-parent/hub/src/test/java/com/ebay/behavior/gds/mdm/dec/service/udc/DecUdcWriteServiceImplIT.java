package com.ebay.behavior.gds.mdm.dec.service.udc;

import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcEntityConverter;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcIngestionService;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.model.enums.CodeLanguageType;
import com.ebay.behavior.gds.mdm.dec.model.enums.DatasetStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalDataModel;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils;

import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.DATASET;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.LDM;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DecUdcWriteServiceImplIT {

    private final int sleepSeconds = 5;

    private final boolean testDeleted = false;
    private final boolean testSignalLineage = false; // manually set to enable/disable signal to ldm lineage ingestion

    private final long createId = 100000001;
    private final long createId2 = 100000002;

    private LdmBaseEntity baseLdm;
    private Namespace baseNamespace;
    private Namespace domainNamespace;
    private LdmBaseEntity derivedLdm;
    private Dataset dataset;
    private PhysicalStorage storage;

    @Autowired
    private UdcEntityConverter entityConverter;

    @Autowired
    private DecUdcWriteServiceImpl writeService;

    @Autowired
    private MetadataReadService readService;

    @Autowired
    private UdcIngestionService ingestService;

    @BeforeEach
    void setUp() {
        // set up base LDM entity, views & fields
        baseNamespace = Namespace.builder().id(1L).name("Base").type(NamespaceType.BASE).owners("gds").build();
        String fieldName = "IT_test_field";
        LdmField field = LdmField.builder().name(fieldName).description("IT_test_description").dataType("string").build();
        LdmEntity baseLdmRaw = LdmEntity.builder().viewType(ViewType.RAW).version(1).fields(Set.of(field)).build();
        LdmEntity baseLdmSnapshot = LdmEntity.builder().viewType(ViewType.SNAPSHOT).version(1).fields(Set.of(field)).build();
        baseLdm = LdmBaseEntity.builder().id(createId).name("IT_test_ldm_name").namespaceId(1L).owners("user")
                .description("IT_test_description").pk("IT_test_pk").views(List.of(baseLdmRaw, baseLdmSnapshot)).build();

        // set up derived LDM entity
        domainNamespace = Namespace.builder().id(2L).name("Domain").type(NamespaceType.DOMAIN).owners("domain").build();
        LdmEntity derivedLdmView = LdmEntity.builder().viewType(ViewType.NONE).version(1).fields(Set.of(field))
                .codeLanguage(CodeLanguageType.PYTHON).codeContent("sample code").build();
        derivedLdm = LdmBaseEntity.builder().id(createId2).name("IT_test_derived_ldm_name").namespaceId(2L).owners("user1,user2")
                .description("IT_test_description").pk("IT_test_pk").views(List.of(derivedLdmView)).build();

        Pipeline pipeline = Pipeline.builder()
                .name("IT_test_pipeline_name")
                .pipelineId("IT_test_pipeline_id").build();
        storage = PhysicalStorage.builder().accessType(AccessType.HIVE)
                .storageDetails("{\"table_name\":\"gds_db.gds_table_name\"}")
                .pipelines(Set.of(pipeline))
                .build();
        // set up dataset
        dataset = Dataset.builder().id(createId).name("IT_test_dataset_name_1").status(DatasetStatus.VALIDATED)
                .owners("user1,user2")
                .ldmVersion(1).ldmEntityId(1L)
                .consumptionParameters("{}")
                .runtimeConfigurations("{}")
                .accessDetails("{}")
                .build();
    }

    @AfterAll
    void tearDown() {
        val cleanupEnabled = true;
        if (cleanupEnabled) {
            writeService.deleteLogicalDataModel(createId);
            writeService.deleteLogicalDataModel(createId2);
            writeService.deleteConsumableDataset(createId);
        }
    }

    @Test
    void upsertAll() {
        var baseLdmId = writeService.upsertLogicalDataModel(baseLdm, baseNamespace, Set.of(), Set.of());
        var derivedLdmId = writeService.upsertLogicalDataModel(derivedLdm, domainNamespace, Set.of(baseLdm.getId()), Set.of());
        var datasetIdMap = writeService.upsertConsumableDataset(dataset, domainNamespace, createId2, "IT_test_ldm_name",
                storage, null);
        TestUtils.sleep(sleepSeconds);

        // validate consumable dataset properties
        validateBasicProperties(baseLdmId, LDM, false);
        validateBasicProperties(derivedLdmId, LDM, false);
        validateBasicProperties(datasetIdMap, DATASET, false);

        // validate lineage
        var lineageId = datasetIdMap.get(TRANSFORMATION);
        assertThat(lineageId).isNotNull();
        var lineageEntity = readService.getEntityById(lineageId);
        assertThat(lineageEntity.getEntityType()).isEqualTo(TRANSFORMATION);
        assertThat(lineageEntity.getGraphPk()).isEqualTo(lineageId);
    }

    @Test
    void upsertSignalLineage() {
        if (testSignalLineage) {
            var baseLdmId = writeService.upsertLogicalDataModel(baseLdm, baseNamespace, Set.of(), Set.of());
            assertThat(baseLdmId).isNotNull();
            var resultMap = writeService.upsertSignalToLdmLineage(baseLdm.getId(), Set.of(1L));
            TestUtils.sleep(sleepSeconds);
            // validate lineage
            var lineageId = resultMap.get(TRANSFORMATION);
            assertThat(lineageId).isNotNull();
            var lineageEntity = readService.getEntityById(lineageId);
            assertThat(lineageEntity.getEntityType()).isEqualTo(TRANSFORMATION);
            assertThat(lineageEntity.getGraphPk()).isEqualTo(lineageId);
            // delete
            var entityId = writeService.deleteLineage(TRANSFORMATION, lineageId);
            validateDeleted(entityId);
        }
    }

    private void validateBasicProperties(Map<UdcEntityType, String> entityIdMap, UdcEntityType entityType, Boolean withRelation) {
        var entityId = entityIdMap.get(entityType);
        validateBasicProperties(entityId, entityType, withRelation);
    }

    private void validateBasicProperties(String entityId, UdcEntityType entityType, Boolean withRelation) {
        var persisted = readService.getEntityById(entityId);
        assertThat(persisted.getGraphPk()).isEqualTo(entityId);
        assertThat(persisted.getEntityType()).isEqualTo(entityType);
        assertThat(persisted.isDeleted()).isFalse();
        assertThat(persisted.getProperties()).isNotEmpty();
        if (withRelation) {
            assertThat(persisted.getRelationships()).isNotEmpty();
        } else {
            assertThat(persisted.getRelationships()).isEmpty();
        }
    }

    @Test
    void deleteLogicalDataModel() {
        long deleteId = 100000003;

        LogicalDataModel ldm = LogicalDataModel.builder().logicalDataModelId(deleteId).logicalDataModelName("IT_test_ldm_name" + deleteId)
                .pk("id").namespace("Base").build();

        writeService.upsertLogicalDataModel(ldm, Set.of());

        var entityId = writeService.deleteLogicalDataModel(deleteId);
        validateDeleted(entityId);
    }

    private void validateDeleted(String entityId) {
        TestUtils.sleep(sleepSeconds);
        val deleted = readService.getEntityById(entityId);

        if (testDeleted) {
            assertThat(deleted.isDeleted()).isTrue();
        }
    }
}
