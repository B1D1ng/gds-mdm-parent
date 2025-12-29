package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimTypeLookup;
import com.ebay.behavior.gds.mdm.signal.service.SignalDimTypeLookupService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;
import static org.assertj.core.api.Assertions.assertThat;

class SignalDimTypeLookupResourceIT extends AbstractResourceTest {

    @Autowired
    private SignalDimTypeLookupService service;

    private SignalDimTypeLookup lookup;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LOOKUP + "/signal_dimension_type";
        lookup = service.getByName("DOMAIN");
    }

    @Test
    void create() {
        var dimension = TestModelUtils.signalDimTypeLookup("test");

        var created = requestSpecWithBody(dimension)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", SignalDimTypeLookup.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var updated = requestSpecWithBody(lookup)
                .when().put(url + '/' + lookup.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalDimTypeLookup.class);

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
        var result = requestSpec()
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", SignalDimTypeLookup.class);

        assertThat(result.size()).isGreaterThanOrEqualTo(1);
        assertThat(result.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getByName() {
        var result = requestSpec()
                .when().get(url + "?name=" + lookup.getName())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", SignalDimTypeLookup.class);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getById() {
        var result = requestSpec()
                .when().get(url + '/' + lookup.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalDimTypeLookup.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(lookup.getId());
    }
}
