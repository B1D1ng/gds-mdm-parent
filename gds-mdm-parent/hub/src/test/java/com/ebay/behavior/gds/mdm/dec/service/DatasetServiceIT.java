package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.DatasetPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.dto.DatasetStatusUpdateRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.DatasetStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.MappingSaveMode;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

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
class DatasetServiceIT {

    @Autowired
    private DatasetService service;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private DatasetPhysicalStorageMappingService mappingService;

    private Dataset dataset;
    private LdmEntity ldmEntity;
    private Namespace namespace;
    private Long namespaceId;

    @BeforeAll
    void setUpAll() {
        namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);
        namespaceId = namespace.getId();

        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        ldmEntity = entityService.create(entity);
    }

    @BeforeEach
    void setUp() {
        dataset = TestModelUtils.dataset(ldmEntity.getId(), ldmEntity.getVersion(), namespaceId);
        dataset = service.create(dataset);
    }

    @Test
    void getById() {
        var persisted = service.getById(VersionedId.of(dataset.getId(), dataset.getVersion()));
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void getById_NotFound() {
        assertThatThrownBy(() -> service.getById(VersionedId.of(1000L, 1000)))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByIdCurrentVersion() {
        var persisted = service.getByIdCurrentVersion(dataset.getId());
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void getByIdCurrentVersion_NotFound() {
        assertThatThrownBy(() -> service.getByIdCurrentVersion(1000L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAllByNameCurrentVersion() {
        var persisted = service.getAllByNameCurrentVersion(dataset.getName());
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllByLdmEntityIdCurrentVersion() {
        var persisted = service.getAllByLdmEntityIdCurrentVersion(ldmEntity.getId(), ldmEntity.getVersion());
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllByLdmEntityId() {
        var persisted = service.getAllByLdmEntityId(ldmEntity.getId());
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllByNameAndNamespaceCurrentVersion() {
        var persisted = service.getAllByNameAndNamespaceCurrentVersion(dataset.getName(), namespace.getName());
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllByNamespaceNameCurrentVersion() {
        var persisted = service.getAllByNamespaceNameCurrentVersion(namespace.getName());
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void searchByNameAndNamespace() {
        var persisted = service.searchByNameAndNamespace(dataset.getName(), namespace.getName());
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);

        var persisted2 = service.searchByNameAndNamespace(dataset.getName(), null);
        assertThat(persisted2.size()).isGreaterThanOrEqualTo(1);

        var persisted3 = service.searchByNameAndNamespace(null, namespace.getName());
        assertThat(persisted3.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void saveAsNewVersion() {
        int originalVersion = dataset.getVersion();
        var saved = service.saveAsNewVersion(dataset);
        var persisted = service.getByIdCurrentVersion(saved.getId());
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getVersion()).isEqualTo(originalVersion + 1);
    }

    @Test
    void update() {
        var originalVersion = dataset.getVersion();
        dataset.setName("New Name");
        // update dataset with changes in current version
        var saved2 = service.update(dataset);
        var persisted2 = service.getByIdCurrentVersion(saved2.getId());
        assertThat(persisted2.getId()).isNotNull();
        assertThat(persisted2.getVersion()).isEqualTo(originalVersion);
        assertThat(persisted2.getName()).isEqualTo("New Name");
    }

    @Test
    void update_VersionNotMatch() {
        var originalVersion = dataset.getVersion();
        dataset.setName("New Name");
        dataset.setVersion(originalVersion + 100);
        assertThatThrownBy(() -> service.update(dataset)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_NoVersion() {
        dataset.setName("New Name");
        dataset.setVersion(null);
        assertThatThrownBy(() -> service.update(dataset)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hasChanges() {
        boolean hasDiff = service.hasChanges(dataset, dataset);
        assertThat(hasDiff).isFalse();

        Dataset dataset2 = TestModelUtils.dataset(ldmEntity.getId(), ldmEntity.getVersion(), namespaceId);
        dataset2.setName("New Name");
        dataset2.setId(dataset.getId());
        dataset2.setVersion(dataset.getVersion());
        boolean hasDiff2 = service.hasChanges(dataset, dataset2);
        assertThat(hasDiff2).isTrue();
    }

    @Test
    void getAll() {
        var datasets = service.getAllCurrentVersion();
        assertThat(datasets.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void updateStatus() {
        DatasetStatusUpdateRequest request = new DatasetStatusUpdateRequest(dataset.getId(), null, "VALIDATED", "user", null);
        var updated = service.updateStatus(dataset.getId(), "VALIDATED", request);
        assertThat(updated.getId()).isEqualTo(dataset.getId());
        assertThat(updated.getStatus()).isEqualTo(DatasetStatus.VALIDATED);
    }

    @Test
    void updateStatus_WithEnv() {
        DatasetStatusUpdateRequest request = new DatasetStatusUpdateRequest(dataset.getId(),
                PlatformEnvironment.PRE_PRODUCTION, "DEPLOYING", "user", null);
        service.updateStatus(dataset.getId(), "DEPLOYING", request);

        // validate dataset
        var persisted = service.getByIdCurrentVersion(dataset.getId());
        assertThat(persisted.getId()).isEqualTo(dataset.getId());
        assertThat(persisted.getStatus()).isEqualTo(DatasetStatus.DEPLOYING);
        assertThat(persisted.getDeployments()).hasSize(1);

        // validate status for pp env
        var persistedDeployment = persisted.getDeployments().iterator().next();
        assertThat(persistedDeployment.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(persistedDeployment.getDatasetVersion()).isEqualTo(dataset.getVersion());
        assertThat(persistedDeployment.getStatus()).isEqualTo(DatasetStatus.DEPLOYING);
        assertThat(persistedDeployment.getEnvironment()).isEqualTo(PlatformEnvironment.PRE_PRODUCTION);

        // update another status
        DatasetStatusUpdateRequest request2 = new DatasetStatusUpdateRequest(dataset.getId(),
                PlatformEnvironment.PRE_PRODUCTION, "DEPLOYED", "user", null);
        service.updateStatus(dataset.getId(), "DEPLOYED", request2);
        persisted = service.getByIdCurrentVersion(dataset.getId());
        assertThat(persisted.getId()).isEqualTo(dataset.getId());
        assertThat(persisted.getStatus()).isEqualTo(DatasetStatus.DEPLOYED);
        assertThat(persisted.getDeployments()).hasSize(1);

        // validate status for pp env
        persistedDeployment = persisted.getDeployments().iterator().next();
        assertThat(persistedDeployment.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(persistedDeployment.getDatasetVersion()).isEqualTo(dataset.getVersion());
        assertThat(persistedDeployment.getStatus()).isEqualTo(DatasetStatus.DEPLOYED);
        assertThat(persistedDeployment.getEnvironment()).isEqualTo(PlatformEnvironment.PRE_PRODUCTION);
    }

    @Test
    void create_NameConflict() {
        Dataset dataset2 = TestModelUtils.dataset(ldmEntity.getId(), ldmEntity.getVersion(), namespaceId);
        dataset2.setName(dataset.getName());
        assertThatThrownBy(() -> service.create(dataset2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_LdmNotExists() {
        Dataset dataset2 = TestModelUtils.dataset(0L, 1, namespaceId);
        assertThatThrownBy(() -> service.create(dataset2)).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void update_InvalidStatus() {
        dataset.setStatus(DatasetStatus.DEPLOYED);
        assertThatThrownBy(() -> service.update(dataset))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_VersionMismatch() {
        dataset.setVersion(1000);
        assertThatThrownBy(() -> service.update(dataset))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_NameConflict() {
        Dataset dataset2 = TestModelUtils.dataset(ldmEntity.getId(), ldmEntity.getVersion(), namespaceId);
        Dataset saved = service.create(dataset2);
        saved.setName(dataset.getName());
        assertThatThrownBy(() -> service.update(saved)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete() {
        var dataset = TestModelUtils.dataset(ldmEntity.getId(), ldmEntity.getVersion(), namespaceId);
        dataset = service.create(dataset);
        var datasetId = dataset.getId();

        var statusChangeRequest = new DatasetStatusUpdateRequest(dataset.getId(),
                PlatformEnvironment.STAGING, "DEPLOYING", "user", null);
        service.updateStatus(datasetId, "DEPLOYING", statusChangeRequest);

        var storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);

        var mapping = TestModelUtils.datasetPhysicalStorageMapping(datasetId, dataset.getVersion(), storage.getId());
        mappingService.create(mapping);

        service.delete(datasetId);
        assertThatThrownBy(() -> service.getByIdCurrentVersion(datasetId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void savePhysicalMappings_ReplaceAll() {
        var storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);

        var mapping = TestModelUtils.datasetPhysicalStorageMapping(dataset.getId(), null, storage.getId());
        service.savePhysicalMappings(dataset.getId(), Set.of(mapping), null);
        var saved = mappingService.getAllByDatasetIdCurrentVersion(dataset.getId());

        assertThat(saved.size()).isEqualTo(1);
        assertThat(saved.get(0).getDatasetId()).isEqualTo(dataset.getId());
        assertThat(saved.get(0).getDatasetVersion()).isEqualTo(dataset.getVersion());
        assertThat(saved.get(0).getPhysicalStorageId()).isEqualTo(storage.getId());

        var storage2 = TestModelUtils.physicalStorage();
        storage2 = storageService.create(storage2);
        var mapping2 = TestModelUtils.datasetPhysicalStorageMapping(dataset.getId(), null, storage2.getId());
        service.savePhysicalMappings(dataset.getId(), Set.of(mapping2), null);
        var saved2 = mappingService.getAllByDatasetIdCurrentVersion(dataset.getId());

        assertThat(saved2.size()).isEqualTo(1);
        assertThat(saved2.get(0).getDatasetId()).isEqualTo(dataset.getId());
        assertThat(saved2.get(0).getDatasetVersion()).isEqualTo(dataset.getVersion());
        assertThat(saved2.get(0).getPhysicalStorageId()).isEqualTo(storage2.getId());
    }

    @Test
    void savePhysicalMappings_Upsert() {
        var storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);

        var mapping = TestModelUtils.datasetPhysicalStorageMapping(dataset.getId(), null, storage.getId());
        service.savePhysicalMappings(dataset.getId(), Set.of(mapping), null);
        var saved = mappingService.getAllByDatasetIdCurrentVersion(dataset.getId());

        assertThat(saved.size()).isEqualTo(1);
        assertThat(saved.get(0).getDatasetId()).isEqualTo(dataset.getId());
        assertThat(saved.get(0).getDatasetVersion()).isEqualTo(dataset.getVersion());
        assertThat(saved.get(0).getPhysicalStorageId()).isEqualTo(storage.getId());

        var storage2 = TestModelUtils.physicalStorage();
        storage2 = storageService.create(storage2);
        var mapping2 = TestModelUtils.datasetPhysicalStorageMapping(dataset.getId(), null, storage2.getId());
        service.savePhysicalMappings(dataset.getId(), Set.of(mapping2), MappingSaveMode.UPSERT);
        var saved2 = mappingService.getAllByDatasetIdCurrentVersion(dataset.getId());

        assertThat(saved2.size()).isEqualTo(2);
        // verify saved2 has both storage 1 and storage 2
        assertThat(saved2).extracting(DatasetPhysicalStorageMapping::getPhysicalStorageId).contains(storage.getId());
        assertThat(saved2).extracting(DatasetPhysicalStorageMapping::getPhysicalStorageId).contains(storage2.getId());
    }
}
