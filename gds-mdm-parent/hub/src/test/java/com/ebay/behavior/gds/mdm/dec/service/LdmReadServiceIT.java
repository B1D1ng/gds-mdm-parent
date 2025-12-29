package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmFieldPhysicalMappingRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
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
class LdmReadServiceIT {

    @Autowired
    private LdmEntityService service;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmReadService readService;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private LdmFieldService fieldService;

    private LdmEntity entity;
    private Namespace namespace;

    @BeforeAll
    void setUpAll() {
        namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);
    }

    @BeforeEach
    void setUp() {
        entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        LdmField field1 = LdmField.builder().name("item_id").description("item id").dataType("decimal(38,0)").build();
        LdmField field2 = LdmField.builder().name("auct_title").description("item title").dataType("string").build();
        entity.setFields(Set.of(field1, field2));
        entity = service.create(entity);
    }

    @Test
    void getByIdWithAssociations() {
        var persisted = readService.getByIdWithAssociations(VersionedId.of(entity.getId(), entity.getVersion()), null);
        assertThat(persisted.getFields().size()).isEqualTo(2);

        var persisted2 = readService.getByIdWithAssociationsCurrentVersion(entity.getId());
        assertThat(persisted2.getFields()).isNotEmpty();
    }

    @Test
    void getByIdWithAssociationsCurrentVersion_WithEnv() {
        val fieldId = entity.getFields().iterator().next().getId();
        PhysicalStorage storage = TestModelUtils.physicalStorage();
        storage.setStorageEnvironment(PlatformEnvironment.STAGING);
        storage = storageService.create(storage);
        LdmFieldPhysicalMappingRequest fieldPhysicalMappingRequest = new LdmFieldPhysicalMappingRequest(
                fieldId, Set.of(storage.getId()), "user", null);
        fieldService.updateFieldPhysicalMappings(entity.getId(), Set.of(fieldPhysicalMappingRequest));

        var persisted2 = readService.getByIdWithAssociationsCurrentVersion(entity.getId(), "staging");
        assertThat(persisted2.getFields()).isNotEmpty();

        var field = persisted2.getFields().stream().filter(f -> f.getId().equals(fieldId)).findFirst().get();
        assertThat(field.getPhysicalStorageMapping().size()).isEqualTo(1);

        var fieldPhysicalMapping = field.getPhysicalStorageMapping().iterator().next();
        assertThat(fieldPhysicalMapping.getLdmFieldId()).isEqualTo(fieldId);
        assertThat(fieldPhysicalMapping.getPhysicalStorageId()).isEqualTo(storage.getId());

        var fieldPhysicalStorage = storageService.getById(fieldPhysicalMapping.getPhysicalStorageId());
        assertThat(fieldPhysicalStorage.getStorageEnvironment()).isEqualTo(PlatformEnvironment.STAGING);
    }

    @Test
    void searchByNameAndNamespace() {
        var entities = readService.searchByNameAndNamespace(entity.getName(), entity.getViewType().toString(), namespace.getName());
        assertThat(entities.size()).isGreaterThanOrEqualTo(1);

        var entities2 = readService.searchByNameAndNamespace(entity.getName(), entity.getViewType().toString(), null);
        assertThat(entities2.size()).isGreaterThanOrEqualTo(1);

        var entities3 = readService.searchByNameAndNamespace(entity.getName(), null, null);
        assertThat(entities3.size()).isGreaterThanOrEqualTo(1);

        var entities4 = readService.searchByNameAndNamespace(entity.getName(), null, namespace.getName());
        assertThat(entities4.size()).isGreaterThanOrEqualTo(1);

        var entities5 = readService.searchByNameAndNamespace(null, null, namespace.getName());
        assertThat(entities5.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getLdmEntityWrapper() {
        var res = readService.getLdmEntityWrapper(entity, false);
        assertThat(res.getEntity()).isNotNull();
    }

    @Test
    void getLdmEntityWrapper_Item() {
        entity.setName("item_raw");
        var res = readService.getLdmEntityWrapper(entity, true);
        assertThat(res.getEntity()).isNotNull();
    }

    @Test
    void getLdmEntityWrapper_Behavior() {
        entity.setName("user_behavior_raw");
        var res = readService.getLdmEntityWrapper(entity, true);
        assertThat(res.getEntity()).isNotNull();
    }

    @Test
    void getLdmEntityWrapper_WithField() {
        var res = readService.getLdmEntityWrapper(entity, true);
        assertThat(res.getEntity()).isNotNull();
    }

    @Test
    void getByIdWithAssociations_NotFound() {
        assertThatThrownBy(() -> readService.getByIdWithAssociations(VersionedId.of(1000L, 1000), null))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByIdWithAssociations_shouldRetrieveDcsLdms_whenDcsEntitiesExist() {
        // Create upstream LDM entity (already created in setUp())
        LdmEntity upstreamEntity = entity;

        // Create DCS entities that reference the upstream entity
        LdmEntity dcsEntity1 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        dcsEntity1.setName("entity1_DeltaChangeStream"); // DCS entity name must end with _DeltaChangeStream
        dcsEntity1.setIsDcs(true);
        // UpstreamLdm should contain only one element
        dcsEntity1.setUpstreamLdm(upstreamEntity.getId().toString());
        // Add dcsFields which is required for DCS entities
        dcsEntity1.setDcsFields(List.of("item_id", "auct_title"));
        LdmField dcsField1 = LdmField.builder().name("dcs_field_1").description("DCS field 1").dataType("string").build();
        dcsEntity1.setFields(Set.of(dcsField1));
        dcsEntity1 = service.create(dcsEntity1);

        LdmEntity dcsEntity2 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        dcsEntity2.setName("entity2_DeltaChangeStream"); // DCS entity name must end with _DeltaChangeStream
        dcsEntity2.setIsDcs(true);
        // UpstreamLdm should contain only one element
        dcsEntity2.setUpstreamLdm(upstreamEntity.getId().toString());
        // Add dcsFields which is required for DCS entities
        dcsEntity2.setDcsFields(List.of("item_id"));
        LdmField dcsField2 = LdmField.builder().name("dcs_field_2").description("DCS field 2").dataType("string").build();
        dcsEntity2.setFields(Set.of(dcsField2));
        dcsEntity2 = service.create(dcsEntity2);

        // Retrieve the upstream entity with associations
        LdmEntity retrievedEntity = readService.getByIdWithAssociationsCurrentVersion(upstreamEntity.getId());

        // Verify dcsLdms contains the correct IDs
        assertThat(retrievedEntity.getDcsLdms()).isNotNull();
        assertThat(retrievedEntity.getDcsLdms()).hasSize(2);
        assertThat(retrievedEntity.getDcsLdms()).contains(dcsEntity1.getId(), dcsEntity2.getId());
    }

    @Test
    void getByIdWithAssociations_shouldHaveEmptyDcsLdms_whenNoDcsEntitiesExist() {
        // Create a new entity with no DCS entities referencing it
        LdmEntity isolatedEntity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        isolatedEntity.setName("isolated_entity");
        LdmField field = LdmField.builder().name("isolated_field").description("Isolated field").dataType("string").build();
        isolatedEntity.setFields(Set.of(field));
        isolatedEntity = service.create(isolatedEntity);

        // Retrieve the entity with associations
        LdmEntity retrievedEntity = readService.getByIdWithAssociationsCurrentVersion(isolatedEntity.getId());

        // Verify dcsLdms is empty
        assertThat(retrievedEntity.getDcsLdms()).isNotNull();
        assertThat(retrievedEntity.getDcsLdms()).isEmpty();
    }
}

