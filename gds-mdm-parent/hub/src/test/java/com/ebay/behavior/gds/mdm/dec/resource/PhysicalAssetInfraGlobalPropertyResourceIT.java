package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfraGlobalProperty;
import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetInfraGlobalPropertyService;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestPhsicalAssetInfraUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PHYSICAL_ASSET_INFRA_GP_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class PhysicalAssetInfraGlobalPropertyResourceIT extends AbstractResourceTest {

    @Autowired
    private PhysicalAssetInfraGlobalPropertyService service;

    private PhysicalAssetInfraGlobalProperty globalProperty;
    private long globalPropertyId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + PHYSICAL_ASSET_INFRA_GP_API;
        globalProperty = TestPhsicalAssetInfraUtils.physicalAssetInfraGlobalProperty();
        service.getByInfraTypeAndPropertyType(globalProperty.getInfraType(), globalProperty.getPropertyType())
                .ifPresent(existing -> service.delete(existing.getId()));
        globalProperty = service.create(globalProperty);
        globalPropertyId = globalProperty.getId();
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + globalPropertyId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAssetInfraGlobalProperty.class);
        assertThat(persisted.getId()).isEqualTo(globalPropertyId);
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
        globalProperty.setPropertyDetails("Updated Property Details");
        var updated = requestSpec()
                .body(globalProperty)
                .when().put(url + '/' + globalPropertyId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAssetInfraGlobalProperty.class);

        assertThat(updated.getId()).isEqualTo(globalPropertyId);
        assertThat(updated.getPropertyDetails()).isEqualTo("Updated Property Details");
    }

    @Test
    void delete() {
        requestSpec()
                .when().delete(url + '/' + globalPropertyId)
                .then().statusCode(NO_CONTENT.value());
    }

    @Test
    void getAll() {
        var persisted = requestSpec().when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfraGlobalProperty.class);

        assertThat(persisted).isNotEmpty();
    }

    @Test
    void getAll_ByInfraType() {
        var persisted = requestSpec().queryParam("infraType", globalProperty.getInfraType())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfraGlobalProperty.class);

        assertThat(persisted).isNotEmpty();
        assertThat(persisted.stream().anyMatch(p -> p.getInfraType().equals(globalProperty.getInfraType()))).isTrue();
    }

    @Test
    void getAll_ByPropertyType() {
        var persisted = requestSpec().queryParam("propertyType", globalProperty.getPropertyType().name())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfraGlobalProperty.class);

        assertThat(persisted).isNotEmpty();
        assertThat(persisted.stream().anyMatch(p -> p.getPropertyType().equals(globalProperty.getPropertyType()))).isTrue();
    }

    @Test
    void getAll_ByInfraTypeAndPropertyType() {
        var persisted = requestSpec()
                .queryParam("infraType", globalProperty.getInfraType())
                .queryParam("propertyType", globalProperty.getPropertyType().name())
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", PhysicalAssetInfraGlobalProperty.class);

        assertThat(persisted).isNotEmpty();
        assertThat(persisted.stream().anyMatch(p -> p.getInfraType().equals(globalProperty.getInfraType()))).isTrue();
        assertThat(persisted.stream().anyMatch(p -> p.getPropertyType().equals(globalProperty.getPropertyType()))).isTrue();
    }

    @Test
    void create() {
        var newGlobalProperty = TestPhsicalAssetInfraUtils.physicalAssetInfraGlobalProperty();
        service.getByInfraTypeAndPropertyType(newGlobalProperty.getInfraType(), newGlobalProperty.getPropertyType())
                .ifPresent(existing -> service.delete(existing.getId()));
        var created = requestSpec().body(newGlobalProperty)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", PhysicalAssetInfraGlobalProperty.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getInfraType()).isEqualTo(newGlobalProperty.getInfraType());
        assertThat(created.getPropertyType()).isEqualTo(newGlobalProperty.getPropertyType());
    }

    @Test
    void create_InvalidData_MissingRequiredFields() {
        // Create a property without required fields
        var invalidProperty = PhysicalAssetInfraGlobalProperty.builder()
                .propertyDetails("Some details")
                .build();

        requestSpec().body(invalidProperty)
                .when().post(url)
                .then().statusCode(BAD_REQUEST.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void update_IdMismatch() {
        // Try to update with mismatched ID in URL and body
        globalProperty.setId(getRandomLong());

        requestSpec()
                .body(globalProperty)
                .when().put(url + '/' + globalPropertyId)
                .then().statusCode(BAD_REQUEST.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void update_NonExistentResource() {
        var nonExistentId = getRandomLong();
        var nonExistentProperty = TestPhsicalAssetInfraUtils.physicalAssetInfraGlobalProperty();
        nonExistentProperty.setId(nonExistentId);

        requestSpec()
                .body(nonExistentProperty)
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
}