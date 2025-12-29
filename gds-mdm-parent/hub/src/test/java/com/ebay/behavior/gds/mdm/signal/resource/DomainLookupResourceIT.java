package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimValueLookup;
import com.ebay.behavior.gds.mdm.signal.service.DomainLookupService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.VI_DOMAIN;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;
import static org.assertj.core.api.Assertions.assertThat;

class DomainLookupResourceIT extends AbstractResourceTest {

    @Autowired
    private DomainLookupService service;

    private SignalDimValueLookup domain;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LOOKUP + "/domain/";
        domain = service.getByName(VI_DOMAIN); // VI value from signal_dim_value_lookup table under data.sql
    }

    @Test
    void create() {
        var testDomain = TestModelUtils.domain("TEST");

        var created = requestSpecWithBody(testDomain)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", SignalDimValueLookup.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var updated = requestSpecWithBody(domain)
                .when().put(url + domain.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalDimValueLookup.class);

        assertThat(updated.getId()).isEqualTo(domain.getId());
    }

    @Test
    void create_nameInUse_error() {
        requestSpecWithBody(domain)
                .when().post(url)
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void getAll() {
        var domain = requestSpec()
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", SignalDimValueLookup.class);

        assertThat(domain.size()).isGreaterThanOrEqualTo(1);
        assertThat(domain.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getByName() {
        var domains = requestSpec()
                .when().get(url + "?name=" + domain.getName())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", SignalDimValueLookup.class);

        assertThat(domains.size()).isEqualTo(1);
        assertThat(domains.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getById() {
        var result = requestSpec()
                .when().get(url + domain.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalDimValueLookup.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(domain.getId());
    }
}
