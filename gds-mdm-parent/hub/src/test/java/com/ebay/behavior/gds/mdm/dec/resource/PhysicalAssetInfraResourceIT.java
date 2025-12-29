package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfra;
import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetInfraService;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestPhsicalAssetInfraUtils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PHYSICAL_ASSET_INFRA_API;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
class PhysicalAssetInfraResourceIT extends AbstractResourceTest {

    @Autowired
    private PhysicalAssetInfraService service;

    private PhysicalAssetInfra infra;
    private long infraId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + PHYSICAL_ASSET_INFRA_API;
        infra = TestPhsicalAssetInfraUtils.physicalAssetInfra();
        List<PhysicalAssetInfra> existing = service.getAllByInfraTypeAndPropertyTypeAndEnvironment(
                infra.getInfraType(),
                infra.getPropertyType(),
                infra.getPlatformEnvironment());
        if (!existing.isEmpty()) {
            existing.stream().map(p -> p.getId()).forEach(id -> {
                service.delete(id);
            });
        }
        infra = service.create(infra);
        infraId = infra.getId();
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + infraId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAssetInfra.class);
        assertThat(persisted.getId()).isEqualTo(infraId);
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
        infra.setPropertyDetails("Updated Property Details");
        var updated = requestSpec()
                .body(infra)
                .when().put(url + '/' + infraId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAssetInfra.class);

        assertThat(updated.getId()).isEqualTo(infraId);
        assertThat(updated.getPropertyDetails()).isEqualTo("Updated Property Details");
    }

    @Test
    void delete() {
        requestSpec()
                .when().delete(url + '/' + infraId)
                .then().statusCode(NO_CONTENT.value());
    }

    @Test
    void getAll() {
        var persisted = requestSpec().when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfra.class);

        assertThat(persisted).isNotEmpty();
    }

    @Test
    void getAll_ByInfraType() {
        var persisted = requestSpec().queryParam("infraType", infra.getInfraType())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfra.class);

        assertThat(persisted).isNotEmpty();
        assertThat(persisted.stream().anyMatch(p -> p.getInfraType().equals(infra.getInfraType()))).isTrue();
    }

    @Test
    void getAll_ByPropertyType() {
        var persisted = requestSpec().queryParam("propertyType", infra.getPropertyType().name())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfra.class);

        assertThat(persisted).isNotEmpty();
        assertThat(persisted.stream().anyMatch(p -> p.getPropertyType().equals(infra.getPropertyType()))).isTrue();
    }

    @Test
    void getAll_ByEnvironment() {
        var persisted = requestSpec().queryParam("environment", infra.getPlatformEnvironment().name())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfra.class);

        assertThat(persisted).isNotEmpty();
        assertThat(persisted.stream().anyMatch(p -> p.getPlatformEnvironment().equals(infra.getPlatformEnvironment()))).isTrue();
    }

    @Test
    void getAll_ByInfraTypeAndPropertyType() {
        var persisted = requestSpec()
                .queryParam("infraType", infra.getInfraType())
                .queryParam("propertyType", infra.getPropertyType().name())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfra.class);

        assertThat(persisted).isNotEmpty();
        assertThat(persisted.stream().anyMatch(p -> p.getInfraType().equals(infra.getInfraType()))).isTrue();
        assertThat(persisted.stream().anyMatch(p -> p.getPropertyType().equals(infra.getPropertyType()))).isTrue();
    }

    @Test
    void create() {
        var newInfra = TestPhsicalAssetInfraUtils.physicalAssetInfra();
        List<PhysicalAssetInfra> existing = service.getAllByInfraTypeAndPropertyTypeAndEnvironment(
                newInfra.getInfraType(),
                newInfra.getPropertyType(),
                newInfra.getPlatformEnvironment());
        if (!existing.isEmpty()) {
            existing.stream().map(AbstractModel::getId).forEach(id -> {
                service.delete(id);
            });
        }
        var created = requestSpec().body(newInfra)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAssetInfra.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getInfraType()).isEqualTo(newInfra.getInfraType());
        assertThat(created.getPropertyType()).isEqualTo(newInfra.getPropertyType());
    }

    @Test
    void create_InvalidData_MissingRequiredFields() {
        // Create a property without required fields
        var invalidInfra = PhysicalAssetInfra.builder()
                .propertyDetails("Some details")
                .build();

        requestSpec().body(invalidInfra)
                .when().post(url)
                .then().statusCode(BAD_REQUEST.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void update_IdMismatch() {
        // Try to update with mismatched ID in URL and body
        infra.setId(getRandomLong());

        requestSpec()
                .body(infra)
                .when().put(url + '/' + infraId)
                .then().statusCode(BAD_REQUEST.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void update_NonExistentResource() {
        var nonExistentId = getRandomLong();
        var nonExistentInfra = TestPhsicalAssetInfraUtils.physicalAssetInfra();
        nonExistentInfra.setId(nonExistentId);

        requestSpec()
                .body(nonExistentInfra)
                .when().put(url + '/' + nonExistentId)
                .then().statusCode(BAD_REQUEST.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void delete_NonExistentResource() {
        var nonExistentId = getRandomLong();

        requestSpec()
                .when().delete(url + '/' + nonExistentId)
                .then().statusCode(NO_CONTENT.value());
    }

    @Test
    void getAll_NonExistentInfraType() {
        assertThatThrownBy(() -> service.getAllByInfraType(InfraType.valueOf("non-existent-type")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAll_ByMultipleParameters() {
        var persisted = requestSpec()
                .queryParam("infraType", infra.getInfraType())
                .queryParam("propertyType", infra.getPropertyType().name())
                .queryParam("environment", infra.getPlatformEnvironment().name())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfra.class);

        assertThat(persisted).isNotEmpty();
        assertThat(persisted.stream().anyMatch(p ->
                p.getInfraType().equals(infra.getInfraType())
                        && p.getPropertyType().equals(infra.getPropertyType())
                        && p.getPlatformEnvironment().equals(infra.getPlatformEnvironment()))).isTrue();
    }
}