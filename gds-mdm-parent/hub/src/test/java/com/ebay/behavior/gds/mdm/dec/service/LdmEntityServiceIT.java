package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmFieldPhysicalMappingRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.StorageContext;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.dec.util.EntityUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LdmEntityServiceIT {

    @Autowired
    private LdmEntityService service;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmReadService readService;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private LdmChangeRequestService changeRequestService;

    private LdmEntity entity;
    private Namespace namespace;

    @BeforeAll
    void setUpAll() {
        namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);
    }

    @BeforeEach
    void setUp() {
        entity = TestModelUtils.ldmEntityWithFieldsAndSignalMapping(namespace.getId());
        entity = service.create(entity);
    }

    @Test
    void saveAsNewVersion() {
        var persisted = readService.getByIdWithAssociations(VersionedId.of(entity.getId(), entity.getVersion()), null);
        int persistedVersion = persisted.getVersion();

        persisted.setDescription("Updated Description");
        var saved1 = service.saveAsNewVersion(persisted, null, false);
        var persisted1 = readService.getByIdWithAssociationsCurrentVersion(saved1.getId());
        assertThat(persisted1.getId()).isNotNull();
        assertThat(persisted1.getVersion()).isEqualTo(persistedVersion + 1);
        assertThat(persisted1.getFields().size()).isEqualTo(2);
    }

    @Test
    void saveAsNewVersion_NamespaceChange() {
        var persisted = readService.getByIdWithAssociations(VersionedId.of(entity.getId(), entity.getVersion()), null);
        int persistedVersion = persisted.getVersion();

        var namespace2 = TestModelUtils.namespace();
        namespace2 = namespaceService.create(namespace2);

        persisted.setNamespaceId(namespace2.getId());
        persisted.setName("Updated Name");
        persisted.setTeam("Updated Team");
        persisted.setTeamDl("Updated TeamDl");
        persisted.setOwners("Updated Owners");
        persisted.setViewType(ViewType.SNAPSHOT);

        var saved1 = service.saveAsNewVersion(persisted, null, false);
        var persisted1 = readService.getByIdWithAssociationsCurrentVersion(saved1.getId());
        assertThat(persisted1.getId()).isNotNull();
        assertThat(persisted1.getVersion()).isEqualTo(persistedVersion + 1);
        assertThat(persisted1.getNamespaceId()).isEqualTo(namespace2.getId());
        assertThat(persisted1.getName()).isEqualTo("updated name");
        assertThat(persisted1.getViewType()).isEqualTo(ViewType.SNAPSHOT);
        assertThat(persisted1.getTeam()).isEqualTo("Updated Team");
        assertThat(persisted1.getTeamDl()).isEqualTo("Updated TeamDl");
        assertThat(persisted1.getOwners()).isEqualTo("Updated Owners");

        var baseEntity = baseEntityService.getById(persisted1.getBaseEntityId());
        assertThat(baseEntity.getId()).isNotNull();
        assertThat(baseEntity.getNamespaceId()).isEqualTo(namespace2.getId());
        assertThat(baseEntity.getName()).isEqualTo("updated name");
        assertThat(persisted1.getTeam()).isEqualTo("Updated Team");
        assertThat(persisted1.getTeamDl()).isEqualTo("Updated TeamDl");
        assertThat(persisted1.getOwners()).isEqualTo("Updated Owners");
    }

    @Test
    void saveAsNewVersion_WithPhysicalMapping() {
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

        var persisted = readService.getByIdWithAssociations(VersionedId.of(entity.getId(), entity.getVersion()), null);
        int persistedVersion = persisted.getVersion();

        // update version by adding a new field
        LdmField field3 = LdmField.builder().name("field3").description("field3").dataType("string").build();
        Set<LdmField> newFields = new HashSet<>(fields);
        newFields.add(field3);

        persisted.setFields(newFields);

        var saved1 = service.saveAsNewVersion(persisted, null, false);
        var persisted1 = readService.getByIdWithAssociationsCurrentVersion(saved1.getId());
        assertThat(persisted1.getId()).isNotNull();
        assertThat(persisted1.getVersion()).isEqualTo(persistedVersion + 1);
        assertThat(persisted1.getFields().size()).isEqualTo(3);

        var persistedFields = persisted1.getFields();
        persistedFields.forEach(field -> {
            boolean isField3 = "field3".equals(field.getName());
            if (!isField3) {
                assertThat(field.getPhysicalStorageMapping().size()).isEqualTo(1);
                assertThat(field.getPhysicalStorageMapping().iterator().next().getPhysicalStorageId()).isEqualTo(storageId);
            } else {
                assertThat(field.getPhysicalStorageMapping().size()).isEqualTo(0);
            }
        });
    }

    @Test
    void create_WithField() {
        var entity2 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        var field1 = TestModelUtils.ldmFieldEmpty();
        var field2 = TestModelUtils.ldmFieldEmpty();
        entity2.setFields(Set.of(field1, field2));
        var saved = service.create(entity2);
        var persisted = readService.getByIdWithAssociations(VersionedId.of(saved.getId(), saved.getVersion()), null);
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void create_NameConflict() {
        var entity2 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity2.setName(entity.getName());
        assertThatThrownBy(() -> service.create(entity2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_NamespaceNotExists() {
        var entity2 = TestModelUtils.ldmEntityEmpty(0L);
        assertThatThrownBy(() -> service.create(entity2))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void update() {
        var originalPersisted = readService.getByIdWithAssociations(VersionedId.of(entity.getId(), entity.getVersion()), null);
        int originalVersion = originalPersisted.getVersion();
        var ldmField = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        originalPersisted.setFields(Set.of(ldmField));

        var storage = TestModelUtils.physicalStorage();
        storage.setStorageContext(StorageContext.SYSTEM_ERROR_HANDLING);
        storage = storageService.create(storage);
        var storageId = storage.getId();

        var mapping = TestModelUtils.ldmErrorHandlingStorageMapping(entity.getId(), entity.getVersion(), storageId);
        originalPersisted.setErrorHandlingStorageMappings(Set.of(mapping));
        // update entity with changes in current version
        originalPersisted.setDescription("Updated Description");
        var saved2 = service.update(originalPersisted);
        var persisted2 = readService.getByIdWithAssociationsCurrentVersion(saved2.getId());
        assertThat(persisted2.getId()).isNotNull();
        assertThat(persisted2.getVersion()).isEqualTo(originalVersion);
        assertThat(persisted2.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void update_NameConflict() {
        var entity2 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity2 = service.create(entity2);

        entity.setName(entity2.getName());
        assertThatThrownBy(() -> service.update(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_VersionNotMatch() {
        var originalPersisted = readService.getByIdWithAssociations(VersionedId.of(entity.getId(), entity.getVersion()), null);
        int originalVersion = originalPersisted.getVersion();
        originalPersisted.setVersion(originalVersion + 100);

        // update entity with changes in current version
        originalPersisted.setDescription("Updated Description");
        assertThatThrownBy(() -> service.update(originalPersisted))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hasChanges() {
        boolean hasDiff = service.hasChanges(entity, entity);
        assertThat(hasDiff).isFalse();

        LdmEntity entity2 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity2.setDescription("Updated Description");
        boolean hasDiff2 = service.hasChanges(entity, entity2);
        assertThat(hasDiff2).isTrue();
    }

    @Test
    void updateBaseEntity() {
        // create base namespace
        var baseNamespace = TestModelUtils.namespace();
        baseNamespace.setType(NamespaceType.BASE);
        baseNamespace = namespaceService.create(baseNamespace);

        // initialize base entity
        var baseEntity = TestModelUtils.ldmBaseEntity(baseNamespace.getId());
        baseEntity = baseEntityService.create(baseEntity);
        var baseEntityId = baseEntity.getId();

        // create view
        var request = TestModelUtils.ldmViewCreateChangeRequest(baseEntityId);
        request = changeRequestService.create(request);
        changeRequestService.approve(request.getId());

        // query entity
        var persisted = baseEntityService.getByIdWithAssociations(baseEntityId);
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getViews().size()).isEqualTo(1);
        assertThat(persisted.getViews().get(0).getName()).isEqualTo(EntityUtils.getLdmName(baseEntity.getName(), ViewType.RAW));

        // update base entity
        baseEntity.setName("UpdatedEntity");
        var updated = service.updateBaseEntity(baseEntity);
        assertThat(updated.getId()).isNotNull();

        // query updated entity
        persisted = baseEntityService.getByIdWithAssociations(baseEntityId);
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getViews().size()).isEqualTo(1);
        assertThat(persisted.getViews().get(0).getName()).isEqualTo(EntityUtils.getLdmName("UpdatedEntity", ViewType.RAW));

        // update namespace
        var updatedBaseEntity = baseEntity;
        updatedBaseEntity.setNamespaceId(namespace.getId());
        assertThatThrownBy(() -> service.updateBaseEntity(updatedBaseEntity)).isInstanceOf(IllegalArgumentException.class);
    }
}
