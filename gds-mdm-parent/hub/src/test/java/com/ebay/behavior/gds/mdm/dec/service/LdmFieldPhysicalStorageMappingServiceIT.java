package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

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
class LdmFieldPhysicalStorageMappingServiceIT {

    @Autowired
    private LdmFieldPhysicalStorageMappingService service;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private PhysicalStorageService storageService;

    private LdmFieldPhysicalStorageMapping mapping;
    private LdmField field;
    private PhysicalStorage storage;

    @BeforeAll
    void setUpAll() {
        Namespace namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        LdmEntity entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);

        field = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field = fieldService.create(field);

        storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);

        mapping = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storage.getId());
        mapping = service.create(mapping);
    }

    @Test
    void getAll() {
        var mappings = service.getAll();
        assertThat(mappings.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void create() {
        var mapping1 = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storage.getId());
        mapping1 = service.create(mapping1);

        var persisted = service.getById(mapping1.getId());
        assertThat(persisted.getId()).isEqualTo(mapping1.getId());
    }

    @Test
    void update() {
        PhysicalStorage storage1 = TestModelUtils.physicalStorage();
        storage1 = storageService.create(storage1);
        mapping.setPhysicalStorageId(storage1.getId());

        var updated = service.update(mapping);
        assertThat(updated.getId()).isEqualTo(mapping.getId());
        assertThat(updated.getPhysicalStorageId()).isEqualTo(storage1.getId());
    }

    @Test
    void getByIdWithAssociations() {
        assertThatThrownBy(() -> service.getByIdWithAssociations(mapping.getId()))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }
}
