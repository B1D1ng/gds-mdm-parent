package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmFieldPhysicalMappingRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmRollbackRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.StatusUpdateRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.LdmStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.StorageContext;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.MIN_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LdmActionServiceIT {

    @Autowired
    private LdmActionService service;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private LdmReadService readService;

    private Namespace namespace;

    @BeforeAll
    void setUpAll() {
        namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);
    }

    @Test
    void delete() {
        // set up entity 1 - parent entity
        var entity1 = TestModelUtils.ldmEntityWithFieldsAndSignalMapping(namespace.getId());
        entity1 = entityService.create(entity1);
        var entityId1 = entity1.getId();

        // set up entity 2 - child entity
        var entity2 = TestModelUtils.ldmEntityEmpty(namespace.getId());

        LdmField field3 = LdmField.builder().name("item_price").description("item price").dataType("decimal(38,4)").build();
        LdmField field4 = LdmField.builder().name("item_image_url").description("item image url").dataType("string").build();
        entity2.setFields(Set.of(field3, field4));
        entity2.setUpstreamLdm(String.valueOf(entity1.getId()));

        entity2 = entityService.create(entity2);
        var entityId2 = entity2.getId();

        // set up entity 3 - child entity of entity 1 and entity 2
        var entity3 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity3.setUpstreamLdm(String.join(",", String.valueOf(entity1.getId()), String.valueOf(entity2.getId())));
        entity3 = entityService.create(entity3);
        var entityId3 = entity3.getId();
        var dataset = TestModelUtils.dataset(entity3.getId(), entity3.getVersion(), namespace.getId());
        dataset = datasetService.create(dataset);
        var datasetId = dataset.getId();

        // delete all
        service.delete(entity1.getId());

        assertThatThrownBy(() -> entityService.getByIdCurrentVersion(entityId1))
                .isInstanceOf(DataNotFoundException.class);
        assertThatThrownBy(() -> entityService.getByIdCurrentVersion(entityId2))
                .isInstanceOf(DataNotFoundException.class);
        assertThatThrownBy(() -> entityService.getByIdCurrentVersion(entityId3))
                .isInstanceOf(DataNotFoundException.class);
        assertThatThrownBy(() -> datasetService.getByIdCurrentVersion(datasetId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void updateStatus() {
        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);
        StatusUpdateRequest request = new StatusUpdateRequest(entity.getId(), "FINALIZED", "user", null);
        var updatedEntity = service.updateStatus(entity.getId(), LdmStatus.FINALIZED, request);

        assertThat(updatedEntity.getStatus()).isEqualTo(LdmStatus.FINALIZED);
    }

    @Test
    void updateStatus_IdNotMatch() {
        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);
        var entityId = entity.getId();
        StatusUpdateRequest request = new StatusUpdateRequest(1000L, "FINALIZED", "user", null);
        assertThatThrownBy(() -> service.updateStatus(entityId, LdmStatus.FINALIZED, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rollback_InvalidVersion() {
        var request = new LdmRollbackRequest(0, "user1");
        assertThatThrownBy(() -> service.rollback(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rollback_MinVersion() {
        var request = new LdmRollbackRequest(null, "user1");
        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);
        var entityId = entity.getId();
        assertThatThrownBy(() -> service.rollback(entityId, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rollback() {
        var request = new LdmRollbackRequest(null, "user1");

        // create entity and build signal mapping and physical mapping
        var entity = TestModelUtils.ldmEntityWithFieldsAndSignalMapping(namespace.getId());
        entity = entityService.create(entity);

        var storage = TestModelUtils.physicalStorage();
        storage.setStorageContext(StorageContext.SYSTEM);
        storage = storageService.create(storage);
        var storageId = storage.getId();

        Set<LdmField> fields = entity.getFields();
        Set<LdmFieldPhysicalMappingRequest> physicalMappingRequests = new HashSet<>();
        fields.forEach(field -> {
            physicalMappingRequests.add(new LdmFieldPhysicalMappingRequest(
                    field.getId(), Set.of(storageId), "user", null));
        });

        fieldService.updateFieldPhysicalMappings(entity.getId(), physicalMappingRequests);

        var persisted = readService.getByIdWithAssociationsCurrentVersion(entity.getId());
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getVersion()).isEqualTo(MIN_VERSION);
        assertThat(persisted.getFields().size()).isEqualTo(2);
        persisted.getFields().forEach(field -> {
            assertThat(field.getId()).isNotNull();
            assertThat(field.getPhysicalStorageMapping().size()).isEqualTo(1);
            if ("item_id".equals(field.getName())) {
                assertThat(field.getSignalMapping().size()).isEqualTo(2);
            }
        });

        // update version by adding a new field
        LdmField field3 = LdmField.builder().name("field3").description("field3").dataType("string").build();
        Set<LdmField> newFields = new HashSet<>(fields);
        newFields.add(field3);

        persisted.setFields(newFields);

        var saved1 = entityService.saveAsNewVersion(persisted, null, false);
        var persisted1 = readService.getByIdWithAssociationsCurrentVersion(saved1.getId());
        assertThat(persisted1.getId()).isNotNull();
        assertThat(persisted1.getVersion()).isEqualTo(2);
        assertThat(persisted1.getFields().size()).isEqualTo(3);
        persisted1.getFields().forEach(field -> {
            assertThat(field.getId()).isNotNull();
            boolean isField3 = "field3".equals(field.getName());
            boolean isItemIdField = "item_id".equals(field.getName());
            if (!isField3) {
                assertThat(field.getPhysicalStorageMapping().size()).isEqualTo(1);
            }
            if (isItemIdField) {
                assertThat(field.getSignalMapping().size()).isEqualTo(2);
            }
        });

        // rollback version
        var saved2 = service.rollback(entity.getId(), request);
        var persisted2 = readService.getByIdWithAssociationsCurrentVersion(saved2.getId());
        assertThat(persisted2.getId()).isNotNull();
        assertThat(persisted2.getVersion()).isEqualTo(3);
        assertThat(persisted2.getFields().size()).isEqualTo(2);
        persisted2.getFields().forEach(field -> {
            assertThat(field.getId()).isNotNull();
            assertThat(field.getPhysicalStorageMapping().size()).isEqualTo(1);
            if ("item_id".equals(field.getName())) {
                assertThat(field.getSignalMapping().size()).isEqualTo(2);
            }
        });
    }
}
