package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldSignalMapping;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmFieldPhysicalMappingRequest;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldSignalMappingRepository;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LdmFieldServiceIT {

    @Autowired
    private LdmFieldService service;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private LdmFieldPhysicalStorageMappingRepository physicalMappingRepository;

    @Autowired
    private LdmFieldSignalMappingRepository signalMappingRepository;

    private LdmEntity entity;
    private LdmField field1;

    @BeforeAll
    void setUpAll() {
        Namespace namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);
    }

    @BeforeEach
    void setUp() {
        field1 = TestModelUtils.ldmField(entity.getId(), entity.getVersion());

        LdmFieldSignalMapping mapping1 = TestModelUtils.ldmFieldSignalMappingEmpty();
        LdmFieldSignalMapping mapping2 = TestModelUtils.ldmFieldSignalMappingEmpty();
        field1.setSignalMapping(Set.of(mapping1, mapping2));

        var storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);
        var physicalMapping = TestModelUtils.ldmFieldPhysicalMapping(field1.getId(), storage.getId());
        field1.setPhysicalStorageMapping(Set.of(physicalMapping));

        LdmField field2 = TestModelUtils.ldmField(entity.getId(), entity.getVersion());

        service.saveAll(entity.getId(), Set.of(field1, field2));
    }

    @Test
    void getByIdWithAssociations() {
        var persisted = service.getByIdWithAssociations(field1.getId());

        assertThat(persisted.getId()).isEqualTo(field1.getId());
        assertThat(persisted.getSignalMapping().size()).isEqualTo(2);
    }

    @Test
    void getAll() {
        var fields = service.getAll();
        assertThat(fields.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllByEntityId() {
        var fields = service.getAllByEntityId(entity.getId());
        assertThat(fields.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void saveAll() {
        var field3 = TestModelUtils.ldmField(entity.getId(), entity.getVersion());

        var fields = service.saveAll(entity.getId(), Set.of(field3));

        assertThat(fields.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void validateName_NameConflict() {
        var field3 = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field3.setName(field1.getName());

        assertThatThrownBy(() -> service.validateName(field3.getName(), List.of(field1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete() {
        service.delete(field1.getId());

        assertThatThrownBy(() -> service.getByIdWithAssociations(field1.getId()))
                .isInstanceOf(DataNotFoundException.class);

        assertThat(signalMappingRepository.findByLdmFieldId(field1.getId()).size()).isEqualTo(0);
        assertThat(physicalMappingRepository.findByLdmFieldId(field1.getId()).size()).isEqualTo(0);
    }

    @Test
    void updateFieldPhysicalMappings_EntityNotMatch() {
        var storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);

        var physicalMappingRequest = new LdmFieldPhysicalMappingRequest(field1.getId(), Set.of(storage.getId()), null, null);

        assertThatThrownBy(() -> service.updateFieldPhysicalMappings(entity.getId() + 1, Set.of(physicalMappingRequest)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateFieldPhysicalStorageMappings() {
        // Setup: create two storages
        var storage1 = storageService.create(TestModelUtils.physicalStorage());
        var storage2 = storageService.create(TestModelUtils.physicalStorage());
        // Setup: create a field with storage1 and storage2 mapping
        var field = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field = service.create(field);
        var mapping1 = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storage1.getId());
        var mapping2 = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storage2.getId());
        var saved = service.updateFieldPhysicalStorageMappings(entity.getId(), Set.of(mapping1, mapping2));
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getId()).isNotNull();
        assertThat(saved.get(1).getId()).isNotNull();
        assertThat(saved.get(0).getLdmFieldId()).isEqualTo(field.getId());
        assertThat(saved.get(1).getLdmFieldId()).isEqualTo(field.getId());
        var storageIdOfField1 = saved.stream().map(LdmFieldPhysicalStorageMapping::getPhysicalStorageId).collect(Collectors.toSet());
        assertThat(storageIdOfField1).containsExactlyInAnyOrder(storage1.getId(), storage2.getId());

        // for field1: Add mapping for storage3, remove mapping for storage1, keep mapping for storage2
        // for field2: Add mapping for storage1
        mapping2.setPhysicalFieldExpression("UpdatedExpression");
        var storage3 = storageService.create(TestModelUtils.physicalStorage());
        var mapping3 = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storage3.getId());
        var field2 = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field2 = service.create(field2);
        var mapping4 = TestModelUtils.ldmFieldPhysicalMapping(field2.getId(), storage1.getId());

        service.updateFieldPhysicalStorageMappings(entity.getId(), Set.of(mapping2, mapping3, mapping4));

        // validate
        var mappingOfField1 = physicalMappingRepository.findByLdmFieldId(field.getId());
        assertThat(mappingOfField1).hasSize(2);
        storageIdOfField1 = mappingOfField1.stream().map(LdmFieldPhysicalStorageMapping::getPhysicalStorageId).collect(Collectors.toSet());
        assertThat(storageIdOfField1).containsExactlyInAnyOrder(storage2.getId(), storage3.getId());
        var savedMapping = mappingOfField1.stream().filter(
                x -> Objects.equals(x.getPhysicalStorageId(), storage2.getId())).findFirst();
        assertThat(savedMapping).isPresent();
        assertThat(savedMapping.get().getPhysicalStorageId()).isEqualTo(storage2.getId());
        assertThat(savedMapping.get().getPhysicalFieldExpression()).isEqualTo("UpdatedExpression");

        var mappingOfField2 = physicalMappingRepository.findByLdmFieldId(field2.getId());
        assertThat(mappingOfField2).hasSize(1);
        assertThat(mappingOfField2.get(0).getPhysicalStorageId()).isEqualTo(storage1.getId());
    }

    @Test
    void updateFieldPhysicalStorageMappings_duplicateMapping() {
        // Setup: create storage
        var storage1 = storageService.create(TestModelUtils.physicalStorage());
        // Setup: create field
        var field = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field = service.create(field);
        var mapping1 = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storage1.getId());
        var mapping2 = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storage1.getId());
        var saved = service.updateFieldPhysicalStorageMappings(entity.getId(), Set.of(mapping1, mapping2));
        // validate
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId()).isNotNull();
        assertThat(saved.get(0).getLdmFieldId()).isEqualTo(field.getId());
        assertThat(saved.get(0).getPhysicalStorageId()).isEqualTo(storage1.getId());
    }

    @Test
    void updateFieldPhysicalStorageMappings_newMappingWithId() {
        // Setup: create storage
        var storage1 = storageService.create(TestModelUtils.physicalStorage());
        // Setup: create field
        var field = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field = service.create(field);
        var mapping1 = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storage1.getId());
        mapping1.setId(1000L);
        var saved = service.updateFieldPhysicalStorageMappings(entity.getId(), Set.of(mapping1));
        // validate
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId()).isNotNull();
        assertThat(saved.get(0).getId()).isNotEqualTo(1000L);
        assertThat(saved.get(0).getLdmFieldId()).isEqualTo(field.getId());
        assertThat(saved.get(0).getPhysicalStorageId()).isEqualTo(storage1.getId());
    }

    @Test
    void updateFieldPhysicalStorageMappings_invalidStorage() {
        var invalidStorageId = Long.MAX_VALUE;
        var mapping = TestModelUtils.ldmFieldPhysicalMapping(field1.getId(), invalidStorageId);
        assertThatThrownBy(() -> service.updateFieldPhysicalStorageMappings(entity.getId(), Set.of(mapping)))
            .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void updateFieldPhysicalStorageMappings_invalidField() {
        var storage = storageService.create(TestModelUtils.physicalStorage());
        var invalidFieldId = Long.MAX_VALUE;
        var mapping = TestModelUtils.ldmFieldPhysicalMapping(invalidFieldId, storage.getId());
        assertThatThrownBy(() -> service.updateFieldPhysicalStorageMappings(entity.getId(), Set.of(mapping)))
            .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void updateFieldPhysicalStorageMappings_entityMismatch() {
        var storage = storageService.create(TestModelUtils.physicalStorage());
        var mapping = TestModelUtils.ldmFieldPhysicalMapping(field1.getId(), storage.getId());
        assertThatThrownBy(() -> service.updateFieldPhysicalStorageMappings(entity.getId() + 1, Set.of(mapping)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
