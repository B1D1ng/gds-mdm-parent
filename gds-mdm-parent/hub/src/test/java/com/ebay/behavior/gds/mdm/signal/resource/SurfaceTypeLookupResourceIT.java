package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.SurfaceTypeLookup;
import com.ebay.behavior.gds.mdm.signal.service.SurfaceTypeLookupService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.surfaceType;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;
import static org.assertj.core.api.Assertions.assertThat;

class SurfaceTypeLookupResourceIT extends AbstractResourceTest {

    @Autowired
    private SurfaceTypeLookupService service;

    private SurfaceTypeLookup lookup;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LOOKUP + "/surface-type/";
        lookup = service.getByName(surfaceType().getName());
    }

    @Test
    void create() {
        var testSurfaceType = SurfaceTypeLookup.builder().name("TEST").readableName("TEST name").build();

        var created = requestSpecWithBody(testSurfaceType)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", SurfaceTypeLookup.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var updated = requestSpecWithBody(lookup)
                .when().put(url + lookup.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SurfaceTypeLookup.class);

        assertThat(updated.getId()).isEqualTo(lookup.getId());
    }

    @Test
    void create_nameInUse_error() {
        requestSpecWithBody(lookup)
                .when().post(url)
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void getAll() {
        var surfaceTypes = requestSpec()
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", SurfaceTypeLookup.class);

        assertThat(surfaceTypes.size()).isGreaterThanOrEqualTo(1);
        assertThat(surfaceTypes.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getByName() {
        var surfaceTypes = requestSpec()
                .when().get(url + "?name=" + lookup.getName())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", SurfaceTypeLookup.class);

        assertThat(surfaceTypes.size()).isEqualTo(1);
        assertThat(surfaceTypes.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getById() {
        var result = requestSpec()
                .when().get(url + lookup.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SurfaceTypeLookup.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(lookup.getId());
    }
}
