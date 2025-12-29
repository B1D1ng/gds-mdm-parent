package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class PingResourceIT extends AbstractResourceTest {

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + "/v1/signal/ping";
    }

    @Test
    void ping() {
        var res = given()
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getString(".");

        assertThat(res).isEqualTo("[ping:pong]");
    }
}
