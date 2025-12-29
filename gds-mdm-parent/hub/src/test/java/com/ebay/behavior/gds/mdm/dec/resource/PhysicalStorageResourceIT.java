package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.repository.DatasetPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.service.DatasetService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmFieldService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalStorageService;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.physicalStorage;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PHYSICAL_STORAGE_METADATA_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class PhysicalStorageResourceIT extends AbstractResourceTest {

    @Autowired
    private PhysicalStorageService service;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private DatasetPhysicalStorageMappingRepository datasetMappingRepository;

    @Autowired
    private LdmFieldPhysicalStorageMappingRepository ldmMappingRepository;

    private PhysicalStorage storage;
    private long storageId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + PHYSICAL_STORAGE_METADATA_API;
        storage = physicalStorage();
        storage = service.create(storage);
        storageId = storage.getId();
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + storageId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalStorage.class);

        assertThat(persisted.getId()).isEqualTo(storageId);
    }

    @Test
    void getById_notFound_417() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void update() {
        storage.setStorageDetails("New Details");
        var updated = requestSpec()
                .body(storage)
                .when().put(url + '/' + storageId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalStorage.class);

        assertThat(updated.getId()).isEqualTo(storageId);
        assertThat(updated.getStorageDetails()).isEqualTo("New Details");
    }

    @Test
    void delete() {
        requestSpec()
                .when().delete(url + '/' + storageId)
                .then().statusCode(NO_CONTENT.value());
    }

    @Test
    void getAll() {
        var persisted = requestSpec().when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalStorage.class);

        assertThat(persisted).isNotEmpty();
    }

    @Test
    void getAll_ByLdm() {
        var namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);

        var field = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field = fieldService.create(field);

        var dataset = TestModelUtils.dataset(entity.getId(), entity.getVersion(), namespace.getId());
        dataset = datasetService.create(dataset);

        var datasetMapping = TestModelUtils.datasetPhysicalStorageMapping(dataset.getId(), dataset.getVersion(), storageId);
        datasetMappingRepository.save(datasetMapping);

        var ldmMapping = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storageId);
        ldmMappingRepository.save(ldmMapping);

        var persisted = requestSpec().queryParam("ldmEntityId", entity.getId())
                .queryParam("exclusive", true)
                .queryParam("cascade", true)
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalStorage.class);

        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAll_ByDataset() {
        var namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);

        var dataset = TestModelUtils.dataset(entity.getId(), entity.getVersion(), namespace.getId());
        dataset = datasetService.create(dataset);

        var datasetMapping = TestModelUtils.datasetPhysicalStorageMapping(dataset.getId(), dataset.getVersion(), storageId);
        datasetMappingRepository.save(datasetMapping);

        var persisted = requestSpec().queryParam("datasetId", dataset.getId())
                .queryParam("exclusive", true)
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalStorage.class);

        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void create() {
        var newStorage = physicalStorage();
        var created = requestSpec().body(newStorage)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalStorage.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void updatePipelineMappings() {
        var pipeline = TestModelUtils.pipeline();
        var updated = requestSpec().body(Set.of(pipeline))
                .when().put(url + '/' + storageId + "/pipeline-mappings")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalStorage.class);

        assertThat(updated.getId()).isEqualTo(storageId);
        assertThat(updated.getPipelines().size()).isGreaterThanOrEqualTo(1);
    }
}