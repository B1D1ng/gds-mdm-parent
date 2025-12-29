package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.DatasetPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
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
class DatasetPhysicalStorageMappingServiceIT {

    @Autowired
    private DatasetPhysicalStorageMappingService service;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private PhysicalStorageService storageService;

    private Dataset dataset;
    private DatasetPhysicalStorageMapping mapping;
    private LdmEntity entity;
    private Namespace namespace;

    @BeforeAll
    void setUpAll() {
        namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);
    }

    @BeforeEach
    void setUp() {
        dataset = TestModelUtils.dataset(entity.getId(), entity.getVersion(), namespace.getId());
        dataset = datasetService.create(dataset);

        PhysicalStorage storage = TestModelUtils.physicalStorage();
        storage.setStorageEnvironment(PlatformEnvironment.STAGING);
        storage = storageService.create(storage);

        mapping = TestModelUtils.datasetPhysicalStorageMapping(dataset.getId(), dataset.getVersion(), storage.getId());
        mapping = service.create(mapping);
    }

    @Test
    void getById() {
        var persisted = service.getById(mapping.getId());
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void getAllByDatasetIdCurrentVersion() {
        var persisted = service.getAllByDatasetIdCurrentVersion(dataset.getId());
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllByDatasetId_NoAvailableDeployment() {
        var dataset2 = TestModelUtils.dataset(entity.getId(), entity.getVersion(), namespace.getId());
        dataset2 = datasetService.create(dataset2);

        var persisted = service.getAllByDatasetId(dataset2.getId(), true);
        assertThat(persisted.size()).isEqualTo(0);
    }

    @Test
    void getAllByDatasetId_MultipleVersions() {
        var dataset3 = TestModelUtils.dataset(entity.getId(), entity.getVersion(), namespace.getId());
        dataset3 = datasetService.create(dataset3);

        var storage3 = TestModelUtils.physicalStorage();
        storage3 = storageService.create(storage3);

        var mapping3 = TestModelUtils.datasetPhysicalStorageMapping(dataset3.getId(), dataset3.getVersion(), storage3.getId());
        service.create(mapping3);

        int version = dataset3.getVersion();
        assertThat(version).isEqualTo(1);

        // update dataset version
        dataset3 = datasetService.saveAsNewVersion(dataset3);

        assertThat(dataset3.getVersion()).isEqualTo(2);

        var persisted = service.getAllByDatasetId(dataset3.getId(), true);
        assertThat(persisted.size()).isEqualTo(1);
        assertThat(persisted.get(0).getDatasetId()).isEqualTo(dataset3.getId());
        assertThat(persisted.get(0).getDatasetVersion()).isEqualTo(1);
    }

    @Test
    void getAllByDatasetId() {
        var persisted = service.getAllByDatasetId(dataset.getId(), true);
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllByDatasetIdAndEnvironment() {
        mapping = service.update(mapping);

        var persisted = service.getAllByDatasetIdAndEnvironment(dataset.getId(), "staging", null);
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void update() {
        PhysicalStorage storage2 = TestModelUtils.physicalStorage();
        storage2 = storageService.create(storage2);
        mapping.setPhysicalStorageId(storage2.getId());

        var updated = service.update(mapping);
        assertThat(updated.getId()).isEqualTo(mapping.getId());
        assertThat(updated.getPhysicalStorageId()).isEqualTo(storage2.getId());
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
