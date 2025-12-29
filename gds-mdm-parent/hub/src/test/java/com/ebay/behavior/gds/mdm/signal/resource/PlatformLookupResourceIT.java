package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;
import static org.assertj.core.api.Assertions.assertThat;

class PlatformLookupResourceIT extends AbstractResourceTest {

    @Autowired
    private PlatformLookupService service;

    private PlatformLookup platform;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LOOKUP + "/platform/";
        platform = service.getByName(CJS);
    }

    @Test
    void create() {
        var testPlatform = TestModelUtils.platform("TEST");

        var created = requestSpecWithBody(testPlatform)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", PlatformLookup.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var updated = requestSpecWithBody(platform)
                .when().put(url + platform.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", PlatformLookup.class);

        assertThat(updated.getId()).isEqualTo(platform.getId());
    }

    @Test
    void create_nameInUse_error() {
        requestSpecWithBody(platform)
                .when().post(url)
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void getAll() {
        var platform = requestSpec()
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", PlatformLookup.class);

        assertThat(platform.size()).isGreaterThanOrEqualTo(1);
        assertThat(platform.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getByName() {
        var platforms = requestSpec()
                .when().get(url + "?name=" + platform.getName())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", PlatformLookup.class);

        assertThat(platforms.size()).isEqualTo(1);
        assertThat(platforms.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getById() {
        var result = requestSpec()
                .when().get(url + platform.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", PlatformLookup.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(platform.getId());
    }
}
