package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalAssetLdmMapping;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetLdmMappingRepository;
import com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetInfraService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetService;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestPhsicalAssetInfraUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.physicalAsset;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PHYSICAL_ASSET_METADATA_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class PhysicalAssetResourceIT extends AbstractResourceTest {

    @Autowired
    private PhysicalAssetService service;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private PhysicalAssetLdmMappingRepository ldmMappingRepository;

    @Autowired
    private PhysicalAssetInfraService infraService;

    private PhysicalAsset asset;
    private long assetId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + PHYSICAL_ASSET_METADATA_API;
        asset = physicalAsset();
        asset = service.create(asset);
        assetId = asset.getId();
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + assetId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAsset.class);
        assertThat(persisted.getId()).isEqualTo(assetId);
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
        asset.setAssetName("New Name");
        var updated = requestSpec()
                .body(asset)
                .when().put(url + '/' + assetId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAsset.class);

        assertThat(updated.getId()).isEqualTo(assetId);
        assertThat(updated.getAssetName()).isEqualTo("New Name");
    }

    @Test
    void delete() {
        requestSpec()
                .when().delete(url + '/' + assetId)
                .then().statusCode(NO_CONTENT.value());
    }

    @Test
    void getAll() {
        var persisted = requestSpec().when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAsset.class);

        assertThat(persisted).isNotEmpty();
    }

    @Test
    void getAll_ByLdmId() {
        var namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        var baseEntity = TestModelUtils.ldmBaseEntity(namespace.getId());
        baseEntity = baseEntityService.create(baseEntity);

        var ldmMapping = new PhysicalAssetLdmMapping(asset, baseEntity);
        ldmMappingRepository.save(ldmMapping);

        var persisted = requestSpec().queryParam("ldmBaseEntityId", baseEntity.getId())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAsset.class);

        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void create() {
        var newAsset = physicalAsset();
        var created = requestSpec().body(newAsset)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAsset.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void updateLdmMappings() {
        var namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        var baseEntity = TestModelUtils.ldmBaseEntity(namespace.getId());
        baseEntity = baseEntityService.create(baseEntity);

        var updated = requestSpec().body(Set.of(baseEntity.getId()))
                .when().put(url + '/' + assetId + "/ldm-mappings")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAsset.class);

        assertThat(updated.getId()).isEqualTo(assetId);
        assertThat(updated.getLdmIds().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void updateInfraMappings() {
        // Create an infra
        var infra = TestPhsicalAssetInfraUtils.physicalAssetInfra();
        infra = infraService.create(infra);

        // Update infra mappings
        var updated = requestSpec().body(Set.of(infra.getId()))
                .when().put(url + '/' + assetId + "/infra-mappings")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAsset.class);

        assertThat(updated.getId()).isEqualTo(assetId);
        assertThat(updated.getAssetInfras()).isNotEmpty();
    }
}