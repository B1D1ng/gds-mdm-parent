package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.DatasetPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.enums.DatasetStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.StorageContext;
import com.ebay.behavior.gds.mdm.dec.service.DatasetPhysicalStorageMappingService;
import com.ebay.behavior.gds.mdm.dec.service.DatasetService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalStorageService;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.datasetPhysicalStorageMapping;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.DATASET_METADATA_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class DatasetResourceIT extends AbstractResourceTest {

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private DatasetPhysicalStorageMappingService mappingService;

    private Dataset dataset;
    private Long datasetId;
    private Integer datasetVersion;
    private Namespace namespace;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + DATASET_METADATA_API;

        namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);

        dataset = TestModelUtils.dataset(entity.getId(), entity.getVersion(), namespace.getId());
        dataset = datasetService.create(dataset);
        datasetId = dataset.getId();
        datasetVersion = dataset.getVersion();
    }

    @Test
    void getById_CurrentVersion() {
        var persisted = requestSpec().when().get(url + '/' + datasetId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Dataset.class);

        assertThat(persisted.getId()).isEqualTo(datasetId);
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + datasetId + "?version=" + datasetVersion)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Dataset.class);

        assertThat(persisted.getId()).isEqualTo(datasetId);
    }

    @Test
    void getById_notFound_417() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void create() {
        var newDataset = TestModelUtils.dataset(dataset.getLdmEntityId(), dataset.getLdmVersion(), namespace.getId());
        var created = requestSpecWithBody(newDataset)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Dataset.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRevision()).isEqualTo(0);
    }

    @Test
    void create_LdmNotExists() {
        var newDataset = TestModelUtils.dataset(getRandomLong(), dataset.getLdmVersion(), namespace.getId());
        requestSpecWithBody(newDataset)
                .when().post(url)
                .then().statusCode(EXPECTATION_FAILED.value())
                .extract().body().jsonPath().getObject(".", ErrorMessageV3.class);
    }

    @Test
    void update() {
        dataset.setName("New Name 1");

        var updated = requestSpecWithBody(dataset)
                .when().put(url + String.format("/%d", datasetId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Dataset.class);

        assertThat(updated.getId()).isEqualTo(datasetId);
        assertThat(updated.getVersion()).isEqualTo(datasetVersion);
    }

    @Test
    void updateStatus() {
        var updated = requestSpec()
                .when().put(url + String.format("/%d", datasetId) + "/status/VALIDATED")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Dataset.class);

        assertThat(updated.getId()).isEqualTo(datasetId);
        assertThat(updated.getVersion()).isEqualTo(datasetVersion);
        assertThat(updated.getStatus()).isEqualTo(DatasetStatus.VALIDATED);
    }

    @Test
    void getAll() {
        var persisted = requestSpec().when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", Dataset.class);

        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAll_ByName() {
        var persisted = requestSpec()
                .queryParam("name", dataset.getName())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", Dataset.class);

        assertThat(persisted.size()).isEqualTo(1);
        assertThat(persisted.get(0).getId()).isEqualTo(datasetId);
    }

    @Test
    void getAll_ByLdmEntityId() {
        var persisted = requestSpec()
                .queryParam("ldmEntityId", dataset.getLdmEntityId())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", Dataset.class);

        assertThat(persisted.size()).isEqualTo(1);
        assertThat(persisted.get(0).getId()).isEqualTo(datasetId);
    }

    @Test
    void getAll_ByLdmEntityIdAndVersion() {
        var persisted = requestSpec()
                .queryParam("ldmEntityId", dataset.getLdmEntityId())
                .queryParam("ldmVersion", dataset.getLdmVersion())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", Dataset.class);

        assertThat(persisted.size()).isEqualTo(1);
        assertThat(persisted.get(0).getId()).isEqualTo(datasetId);
    }

    @Test
    void getPhysicalMappingByDatasetId() {
        var storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);

        var mapping = datasetPhysicalStorageMapping(datasetId, datasetVersion, storage.getId());
        mappingService.create(mapping);

        var persisted = requestSpec().when().get(url + '/' + datasetId + "/physical-mappings")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", DatasetPhysicalStorageMapping.class);

        assertThat(persisted.size()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getPhysicalMappingByDatasetId_withSystemContext() {
        // Create storage with SYSTEM context
        var systemStorage = TestModelUtils.physicalStorage();
        systemStorage.setStorageContext(StorageContext.SYSTEM);
        systemStorage = storageService.create(systemStorage);

        // Create storage with DATASET context
        var datasetStorage = TestModelUtils.physicalStorage();
        datasetStorage.setStorageContext(StorageContext.DATASET);
        datasetStorage = storageService.create(datasetStorage);

        // Create storage with SYSTEM_ERROR_HANDLING context
        var errorHandlingStorage = TestModelUtils.physicalStorage();
        errorHandlingStorage.setStorageContext(StorageContext.SYSTEM_ERROR_HANDLING);
        errorHandlingStorage = storageService.create(errorHandlingStorage);

        // Create mappings for each storage
        var systemMapping = datasetPhysicalStorageMapping(datasetId, datasetVersion, systemStorage.getId());
        mappingService.create(systemMapping);

        var datasetMapping = datasetPhysicalStorageMapping(datasetId, datasetVersion, datasetStorage.getId());
        mappingService.create(datasetMapping);

        var errorHandlingMapping = datasetPhysicalStorageMapping(datasetId, datasetVersion, errorHandlingStorage.getId());
        mappingService.create(errorHandlingMapping);

        // Test with single context
        var systemContextResult = requestSpec()
                .queryParam("system_context", "SYSTEM")
                .when().get(url + '/' + datasetId + "/physical-mappings")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", DatasetPhysicalStorageMapping.class);

        assertThat(systemContextResult.size()).isEqualTo(1);
        assertThat(systemContextResult.get(0).getPhysicalStorageId()).isEqualTo(systemStorage.getId());

        // Test with multiple contexts (comma-separated)
        var multipleContextsResult = requestSpec()
                .queryParam("system_context", "SYSTEM,DATASET")
                .when().get(url + '/' + datasetId + "/physical-mappings")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", DatasetPhysicalStorageMapping.class);

        assertThat(multipleContextsResult.size()).isEqualTo(2);
        assertThat(multipleContextsResult.stream().map(DatasetPhysicalStorageMapping::getPhysicalStorageId))
                .containsExactlyInAnyOrder(systemStorage.getId(), datasetStorage.getId());

        // Test with invalid context
        requestSpec()
                .queryParam("system_context", "INVALID_CONTEXT")
                .when().get(url + '/' + datasetId + "/physical-mappings")
                .then().statusCode(HttpStatus.BAD_REQUEST.value());

        // Test with mixed valid and invalid contexts (should return BAD_REQUEST)
        requestSpec()
                .queryParam("system_context", "SYSTEM,INVALID_CONTEXT")
                .when().get(url + '/' + datasetId + "/physical-mappings")
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void updatePhysicalMappings() {
        // create a storage and log a mapping
        var storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);

        var mapping = datasetPhysicalStorageMapping(datasetId, datasetVersion, storage.getId());
        mappingService.create(mapping);

        // create a new storage
        var newStorage2 = TestModelUtils.physicalStorage();
        newStorage2 = storageService.create(newStorage2);
        var newMapping = datasetPhysicalStorageMapping(datasetId, datasetVersion, newStorage2.getId());

        var updated = requestSpecWithBody(Set.of(mapping, newMapping))
                .when().put(url + String.format("/%d/physical-mappings", datasetId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", DatasetPhysicalStorageMapping.class);

        assertThat(updated.size()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void createNewVersion() {
        dataset.setName("New Name");

        var created = requestSpecWithBody(dataset)
                .when().post(url + String.format("/%d", datasetId) + "/version")
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Dataset.class);

        assertThat(created.getId()).isEqualTo(datasetId);
        assertThat(created.getVersion()).isEqualTo(datasetVersion + 1);
    }

    @Test
    void delete() {
        var dataset1 = TestModelUtils.dataset(dataset.getLdmEntityId(), dataset.getLdmVersion(), namespace.getId());
        dataset1 = datasetService.create(dataset1);

        requestSpec()
                .when().delete(url + String.format("/%d", dataset1.getId()))
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }
}
