package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmErrorHandlingStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmEntityWrapper;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmFieldPhysicalMappingRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmRollbackRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.LdmStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.StorageContext;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmErrorHandlingStorageMappingService;
import com.ebay.behavior.gds.mdm.dec.service.LdmFieldService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmBaseEntity;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmEntityEmpty;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmEntityRequest;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmField;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmFieldPhysicalMapping;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.namespace;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.physicalAsset;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.physicalStorage;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_METADATA_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class LdmEntityResourceIT extends AbstractResourceTest {

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmEntityService service;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private PhysicalAssetService assetService;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private LdmErrorHandlingStorageMappingService errorStorageMappingService;

    private Long namespaceId;
    private LdmEntity entity;
    private Long entityId;
    private Integer entityVersion;
    private PhysicalStorage stagingStorage;
    private PhysicalStorage productionStorage;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LDM_METADATA_API;
        Namespace namespace = namespace();
        namespace = namespaceService.create(namespace);
        namespaceId = namespace.getId();

        entity = ldmEntityEmpty(namespaceId);
        entity = service.create(entity);
        entityId = entity.getId();
        entityVersion = entity.getVersion();

        // Create physical asset for error handling storage
        PhysicalAsset asset = physicalAsset();
        asset = assetService.create(asset);

        // Create physical storages for error handling
        stagingStorage = physicalStorage();
        stagingStorage.setPhysicalAssetId(asset.getId());
        stagingStorage.setStorageEnvironment(PlatformEnvironment.STAGING);
        stagingStorage.setStorageContext(StorageContext.SYSTEM_ERROR_HANDLING);
        stagingStorage = storageService.create(stagingStorage);

        productionStorage = physicalStorage();
        productionStorage.setPhysicalAssetId(asset.getId());
        productionStorage.setStorageEnvironment(PlatformEnvironment.PRODUCTION);
        productionStorage.setStorageContext(StorageContext.SYSTEM_ERROR_HANDLING);
        productionStorage = storageService.create(productionStorage);
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + entityId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmEntityWrapper.class);

        assertThat(persisted.getEntity()).isNotNull();
    }

    @Test
    void getById_ByVersion() {
        var version = entity.getVersion();
        var persisted = requestSpec().when().get(url + '/' + entityId + "?version=" + version)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmEntityWrapper.class);
        assertThat(persisted.getEntity()).isNotNull();
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
        var request = ldmEntityRequest(namespaceId);
        var created = requestSpecWithBody(request)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmEntity.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRevision()).isEqualTo(0);
    }

    @Test
    void update() {
        var request = ldmEntityRequest(namespaceId);
        var entity = service.create(request.toLdmEntity());

        request.setId(entity.getId());
        request.setRevision(entity.getRevision());
        request.setVersion(entity.getVersion());
        request.setDescription("Updated Description");
        var updated = requestSpecWithBody(request)
                .when().put(url + String.format("/%d", entity.getId()))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmEntity.class);

        assertThat(updated.getId()).isEqualTo(entity.getId());
        assertThat(updated.getVersion()).isEqualTo(entity.getVersion());
        assertThat(updated.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void getAll() {
        var saved = requestSpec().when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmEntity.class);

        assertThat(saved.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAll_ByNameAndType() {
        var saved = requestSpec().when().get(url + "?name=" + entity.getName() + "&viewType=" + entity.getViewType())
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmEntity.class);

        assertThat(saved.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void createNewVersion() {
        var request = ldmEntityRequest(namespaceId);
        var entity = service.create(request.toLdmEntity());

        request.setId(entity.getId());
        request.setRevision(entity.getRevision());
        request.setVersion(entity.getVersion());
        request.setDescription("Updated Description");

        var created = requestSpecWithBody(request)
                .when().post(url + '/' + entity.getId() + "/version")
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmEntity.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getVersion()).isEqualTo(entity.getVersion() + 1);
        assertThat(created.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void updateStatus() {
        var updated = requestSpec()
                .when().put(url + String.format("/%d", entityId) + "/status/FINALIZED")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmEntity.class);

        assertThat(updated.getId()).isEqualTo(entityId);
        assertThat(updated.getVersion()).isEqualTo(entityVersion);
        assertThat(updated.getStatus()).isEqualTo(LdmStatus.FINALIZED);
    }

    @Test
    void initialize() {
        var entityInitialization = ldmBaseEntity(namespaceId);
        var created = requestSpecWithBody(entityInitialization)
                .when().post(url + "/initialize")
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmBaseEntity.class);
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void updateFields() {
        var field1 = ldmField(entityId, entityVersion);
        var field2 = ldmField(entityId, entityVersion);

        var updated = requestSpecWithBody(Set.of(field1, field2))
                .when().put(url + String.format("/%d", entityId) + "/fields")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmField.class);
        assertThat(updated.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void updateFieldPhysicalMapping() {
        var field1 = ldmField(entityId, entityVersion);
        field1 = fieldService.create(field1);

        var storage = physicalStorage();
        storage = storageService.create(storage);

        var request = new LdmFieldPhysicalMappingRequest(field1.getId(), Set.of(storage.getId()), null, null);

        var updated = requestSpecWithBody(Set.of(request))
                .when().put(url + String.format("/%d", entityId) + "/fields/physical-mappings")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmFieldPhysicalStorageMapping.class);

        assertThat(updated.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void updateFieldPhysicalStorageMapping() {
        var field1 = ldmField(entityId, entityVersion);
        field1 = fieldService.create(field1);

        var storage = physicalStorage();
        storage = storageService.create(storage);

        var request = ldmFieldPhysicalMapping(field1.getId(), storage.getId());

        var updated = requestSpecWithBody(Set.of(request))
                .when().put(url + String.format("/%d", entityId) + "/fields/physical-storage-mappings")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmFieldPhysicalStorageMapping.class);

        assertThat(updated.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void delete() {
        var entity = ldmEntityEmpty(namespaceId);
        entity = service.create(entity);

        requestSpec()
                .when().delete(url + String.format("/%d", entity.getId()))
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void rollback() {
        var originalName = entity.getName();
        entity.setName("New Name");
        service.saveAsNewVersion(entity, null, false);

        var request = new LdmRollbackRequest(1, "user1");

        var rollbackEntity = requestSpecWithBody(request)
                .when().put(url + String.format("/%d/rollback", entityId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmEntity.class);

        assertThat(rollbackEntity.getId()).isEqualTo(entityId);
        assertThat(rollbackEntity.getVersion()).isEqualTo(3);
        assertThat(rollbackEntity.getName()).isEqualTo(originalName);
    }

    @Test
    void updateErrorHandlingPhysicalMappings() {
        // Create mappings to update
        Set<LdmErrorHandlingStorageMapping> mappings = new HashSet<>();

        LdmErrorHandlingStorageMapping mapping1 = new LdmErrorHandlingStorageMapping();
        mapping1.setPhysicalStorageId(stagingStorage.getId());
        mappings.add(mapping1);

        LdmErrorHandlingStorageMapping mapping2 = new LdmErrorHandlingStorageMapping();
        mapping2.setPhysicalStorageId(productionStorage.getId());
        mappings.add(mapping2);

        // Update error handling physical mappings
        var result = requestSpecWithBody(mappings)
                .when().put(url + String.format("/%d/error-handling-physical-mappings", entityId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmErrorHandlingStorageMapping.class);

        // Verify results
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(LdmErrorHandlingStorageMapping::getPhysicalStorageId))
                .containsExactlyInAnyOrder(stagingStorage.getId(), productionStorage.getId());
        assertThat(result.stream().map(LdmErrorHandlingStorageMapping::getLdmEntityId))
                .containsOnly(entityId);
        assertThat(result.stream().map(LdmErrorHandlingStorageMapping::getLdmVersion))
                .containsOnly(entityVersion);
    }

    @Test
    void getErrorHandlingPhysicalMappings() {
        // First create some mappings
        Set<LdmErrorHandlingStorageMapping> mappings = new HashSet<>();

        LdmErrorHandlingStorageMapping mapping1 = new LdmErrorHandlingStorageMapping();
        mapping1.setPhysicalStorageId(stagingStorage.getId());
        mappings.add(mapping1);

        LdmErrorHandlingStorageMapping mapping2 = new LdmErrorHandlingStorageMapping();
        mapping2.setPhysicalStorageId(productionStorage.getId());
        mappings.add(mapping2);

        errorStorageMappingService.saveErrorHandlingStorageMappings(entityId, entityVersion, mappings);

        // Get error handling physical mappings (current version)
        var result = requestSpec()
                .when().get(url + String.format("/%d/error-handling-physical-mappings", entityId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmErrorHandlingStorageMapping.class);

        // Verify results
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(LdmErrorHandlingStorageMapping::getPhysicalStorageId))
                .containsExactlyInAnyOrder(stagingStorage.getId(), productionStorage.getId());
    }

    @Test
    void getErrorHandlingPhysicalMappings_withVersion() {
        // First create some mappings
        Set<LdmErrorHandlingStorageMapping> mappings = new HashSet<>();

        LdmErrorHandlingStorageMapping mapping1 = new LdmErrorHandlingStorageMapping();
        mapping1.setPhysicalStorageId(stagingStorage.getId());
        mappings.add(mapping1);

        errorStorageMappingService.saveErrorHandlingStorageMappings(entityId, entityVersion, mappings);

        // Get error handling physical mappings with version
        var result = requestSpec()
                .when().get(url + String.format("/%d/error-handling-physical-mappings?version=%d", entityId, entityVersion))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmErrorHandlingStorageMapping.class);

        // Verify results
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhysicalStorageId()).isEqualTo(stagingStorage.getId());
    }

    @Test
    void getErrorHandlingPhysicalMappings_withEnvironment() {
        // First create some mappings
        Set<LdmErrorHandlingStorageMapping> mappings = new HashSet<>();

        LdmErrorHandlingStorageMapping mapping1 = new LdmErrorHandlingStorageMapping();
        mapping1.setPhysicalStorageId(stagingStorage.getId());
        mappings.add(mapping1);

        LdmErrorHandlingStorageMapping mapping2 = new LdmErrorHandlingStorageMapping();
        mapping2.setPhysicalStorageId(productionStorage.getId());
        mappings.add(mapping2);

        errorStorageMappingService.saveErrorHandlingStorageMappings(entityId, entityVersion, mappings);

        // Get error handling physical mappings with environment
        var result = requestSpec()
                .when().get(url + String.format("/%d/error-handling-physical-mappings?env=STAGING", entityId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmErrorHandlingStorageMapping.class);

        // Verify results
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhysicalStorageId()).isEqualTo(stagingStorage.getId());
    }
}
