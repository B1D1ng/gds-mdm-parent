package com.ebay.behavior.gds.mdm.signal.common.service.udc;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.UdcWriteServiceImpl;
import com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils;

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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.ATTRIBUTE;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.COLUMN_TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.EVENT;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.FIELD;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.PAGE_VIEW_ENTRY;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.PAGE_VIEW_EXIT;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UdcWriteServiceImplIT {

    private final int sleepSeconds = 5;

    // We cannot test delete() functionality without large waiting time, since UDC Portal might be slow.
    // So this flag set to disable some checks on every build. But we still can enable it manually, and so to test delete() correctly
    private final boolean testDeleted = false;

    private final long createId = 22L;
    private final long deleteId = 2L;
    private final long deleteId1 = 3L;

    Map<UdcEntityType, String> signalIdMap;
    Map<UdcEntityType, String> fieldIdMap;

    private VersionedId signalId = VersionedId.of(createId, MIN_VERSION);

    private UnstagedEvent event;
    private UnstagedSignal signal;
    private UnstagedAttribute attribute;
    private UnstagedField field;

    @Autowired
    private UdcWriteServiceImpl writeService;

    @Autowired
    private MetadataReadService readService;

    @BeforeEach
    void setUp() {
        initTestSetup(createId);
    }

    /**
     * the setup must include all associated objects for promote to work,
     * since promote algorithm uses associated objects right from embedded sets.
     * For example event must have attributes set populated, and signal must have events/fields sets populated.
     */
    void initTestSetup(long id) {
        // the setup must include all associated objects for promote to work,
        // since promote algorithm uses associated objects right from embedded sets.
        // For example event must have attributes set populated, and signal must have events/fields sets populated.
        signalId = VersionedId.of(id, MIN_VERSION + 1);

        attribute = unstagedAttribute(id).toBuilder()
                .id(id)
                .tag("IT_test_tag_" + id)
                .build();

        event = unstagedEvent().toBuilder()
                .id(id)
                .name("IT_test_event_" + id)
                .description("IT_test_description_" + id)
                .attributes(Set.of(attribute))
                .pageIds(Set.of(1L, 2L))
                .moduleIds(Set.of(3L, 4L))
                .clickIds(Set.of(5L, 6L))
                .build();

        field = unstagedField(signalId).toBuilder()
                .id(id)
                .name("IT_test_field_" + id)
                .description("IT_test_description_" + id)
                .eventTypes(String.join(COMMA, PAGE_VIEW_ENTRY, PAGE_VIEW_EXIT))
                .attributes(Set.of(attribute))
                .build();

        signal = unstagedSignal(id).toBuilder()
                .signalSourceId(123L)
                .signalSourceVersion(1)
                .name("IT_test_signal_" + id)
                .description("IT_test_description_" + id)
                .type("PAGE_IMPRESSION")
                .legacyId("IT_test_legacy_id_" + id)
                .uuidGeneratorType("uuid_generator_type")
                .uuidGeneratorExpression("uuid_generator_expression")
                .correlationIdExpression("correlation_id_expression")
                .needAccumulation(true)
                .events(Set.of(event))
                .fields(Set.of(field))
                .build();
        signal.setSignalId(signalId);
    }

    @AfterAll
    void tearDown() {
        val cleanupEnabled = false;
        if (cleanupEnabled) { // manually set to enable/disable a cleanup
            if (Objects.nonNull(signalIdMap)) {
                var lineageId = signalIdMap.get(TRANSFORMATION);
                if (Objects.nonNull(lineageId)) {
                    writeService.delete(TRANSFORMATION, lineageId);
                }
            }

            if (Objects.nonNull(fieldIdMap)) {
                var lineageId = fieldIdMap.get(COLUMN_TRANSFORMATION);
                if (Objects.nonNull(lineageId)) {
                    writeService.delete(COLUMN_TRANSFORMATION, lineageId);
                }
            }

            writeService.deleteSignal(createId);
            writeService.deleteEvent(createId);
            writeService.deleteAttribute(createId);
            writeService.deleteField(createId);
        }

        writeService.deleteSignal(deleteId);
        writeService.deleteEvent(deleteId);
        writeService.deleteAttribute(deleteId);
        writeService.deleteField(deleteId);
    }

    @Test
    void upsert() {
        var id = createId + 1;
        initTestSetup(id); // not reusing createId to avoid conflicts with previous tests

        var signalEntityId = writeService.upsert(signal, UdcDataSourceType.TEST);
        TestUtils.sleep(sleepSeconds);

        var attribute1 = readService.getById(ATTRIBUTE, id, UnstagedAttribute.class);
        var event1 = readService.getById(EVENT, id, UnstagedEvent.class);
        var field1 = readService.getById(FIELD, id, UnstagedField.class);
        var signal1 = readService.getById(SIGNAL, id, UnstagedSignal.class);

        assertThat(signalEntityId).isNotBlank();
        assertThat(event1.getId()).isEqualTo(id);
        assertThat(attribute1.getId()).isEqualTo(id);
        assertThat(field1.getId()).isEqualTo(id);
        assertThat(signal1.getId()).isEqualTo(id);
        assertThat(signal1.getVersion()).isEqualTo(signalId.getVersion());
        assertThat(signal1.getEvents().size()).isEqualTo(1);

        // test delete logic worked correctly
        id++;
        initTestSetup(id);

        writeService.upsert(signal, UdcDataSourceType.TEST);
        TestUtils.sleep(sleepSeconds);

        signal1 = readService.getById(SIGNAL, id, UnstagedSignal.class);
        assertThat(signal1.getId()).isEqualTo(id);
        assertThat(signal1.getVersion()).isEqualTo(signalId.getVersion());
        assertThat(signal1.getEvents().size()).isEqualTo(1);
    }

    @Test
    void upsertSignal() {
        var eventEntityId = writeService.upsertEvent(event);
        var attrEntityId = writeService.upsertAttribute(createId, attribute);
        signalIdMap = writeService.upsertSignal(Set.of(createId), signal);
        fieldIdMap = writeService.upsertField(signalId.getId(), Set.of(createId), field);
        TestUtils.sleep(sleepSeconds);

        validateAttribute(attrEntityId);
        validateEvent(eventEntityId);
        validateSignal(signalIdMap);

        var entityId = fieldIdMap.get(FIELD);
        var persisted = readService.getEntityById(entityId);
        assertThat(persisted.getGraphPk()).isEqualTo(entityId);
        assertThat(persisted.getEntityType()).isEqualTo(FIELD);
        assertThat(persisted.isDeleted()).isFalse();
        assertThat(persisted.getProperties()).isNotEmpty();
        assertThat(persisted.getRelationships()).isNotEmpty();

        var lineageId = fieldIdMap.get(COLUMN_TRANSFORMATION);
        assertThat(lineageId).isNotNull();
        var lineageEntity = readService.getEntityById(lineageId); // We cannot query getLineageById since it takes time to be written into GraphDB
        assertThat(lineageEntity.getEntityType()).isEqualTo(COLUMN_TRANSFORMATION);
        assertThat(lineageEntity.getGraphPk()).isEqualTo(lineageId);
    }

    @Test
    void deleteSignal() {
        event = unstagedEvent().toBuilder()
                .id(deleteId)
                .attributes(Set.of())
                .pageIds(Set.of())
                .moduleIds(Set.of())
                .clickIds(Set.of())
                .build();
        attribute = unstagedAttribute(deleteId).toBuilder().id(deleteId).build();
        field = unstagedField(VersionedId.of(deleteId, MIN_VERSION)).toBuilder()
                .id(deleteId)
                .attributes(Set.of())
                .build();
        signal = unstagedSignal(deleteId).toBuilder()
                .events(Set.of(event))
                .fields(Set.of(field))
                .build();
        signal.setSignalId(VersionedId.of(deleteId, MIN_VERSION));

        writeService.upsertEvent(event);
        writeService.upsertAttribute(deleteId, attribute);
        writeService.upsertSignal(Set.of(deleteId), signal);
        writeService.upsertField(deleteId, Set.of(deleteId), field);

        var entityId = writeService.deleteField(deleteId);
        validateDeleted(entityId);

        entityId = writeService.deleteAttribute(deleteId);
        validateDeleted(entityId);

        entityId = writeService.deleteEvent(deleteId);
        validateDeleted(entityId);

        entityId = writeService.deleteSignal(deleteId);
        validateDeleted(entityId);
    }

    @Test
    void deleteSignalWithAssociations() {
        event = unstagedEvent().toBuilder()
                .id(deleteId)
                .attributes(Set.of())
                .pageIds(Set.of())
                .moduleIds(Set.of())
                .clickIds(Set.of())
                .build();
        attribute = unstagedAttribute(deleteId1).toBuilder().id(deleteId1).build();
        field = unstagedField(VersionedId.of(deleteId1, MIN_VERSION)).toBuilder()
                .id(deleteId1)
                .attributes(Set.of())
                .build();
        signal = unstagedSignal(deleteId1).toBuilder()
                .events(Set.of(event))
                .fields(Set.of(field))
                .build();
        signal.setSignalId(VersionedId.of(deleteId1, MIN_VERSION));

        writeService.upsertEvent(event);
        writeService.upsertAttribute(deleteId1, attribute);
        writeService.upsertSignal(Set.of(deleteId1), signal);
        writeService.upsertField(deleteId1, Set.of(deleteId1), field);

        var entityId = writeService.deleteSignalWithAssociations(deleteId1, UdcDataSourceType.TEST);
        validateDeleted(entityId);
    }

    private void validateDeleted(String entityId) {
        TestUtils.sleep(sleepSeconds);
        val deleted = readService.getEntityById(entityId);

        if (testDeleted) {
            assertThat(deleted.isDeleted()).isTrue();
        }
    }

    private void validateAttribute(String entityId) {
        var persisted = readService.getEntityById(entityId);
        assertThat(persisted.getGraphPk()).isEqualTo(entityId);
        assertThat(persisted.getEntityType()).isEqualTo(ATTRIBUTE);
        assertThat(persisted.isDeleted()).isFalse();
        assertThat(persisted.getProperties()).isNotEmpty();
        assertThat(persisted.getRelationships()).isNotEmpty();
    }

    private void validateEvent(String entityId) {
        var persisted = readService.getEntityById(entityId);
        assertThat(persisted.getGraphPk()).isEqualTo(entityId);
        assertThat(persisted.getEntityType()).isEqualTo(EVENT);
        assertThat(persisted.isDeleted()).isFalse();
        assertThat(persisted.getProperties()).isNotEmpty();
        assertThat(persisted.getRelationships()).isEmpty();
    }

    private void validateSignal(Map<UdcEntityType, String> entityIdMap) {
        var entityId = entityIdMap.get(SIGNAL);
        var persisted = readService.getEntityById(entityId);
        assertThat(persisted.getGraphPk()).isEqualTo(entityId);
        assertThat(persisted.getEntityType()).isEqualTo(SIGNAL);
        assertThat(persisted.isDeleted()).isFalse();
        assertThat(persisted.getProperties()).isNotEmpty();
        assertThat(persisted.getRelationships()).isEmpty();

        var lineageId = entityIdMap.get(TRANSFORMATION);
        assertThat(lineageId).isNotNull();
        var lineageEntity = readService.getEntityById(lineageId); // We cannot query getLineageById since it takes time to be written into GraphDB
        assertThat(lineageEntity.getEntityType()).isEqualTo(TRANSFORMATION);
        assertThat(lineageEntity.getGraphPk()).isEqualTo(lineageId);
    }
}