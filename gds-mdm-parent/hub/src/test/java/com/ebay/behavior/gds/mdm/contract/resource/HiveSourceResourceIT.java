package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.service.HiveSourceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveSource;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;

class HiveSourceResourceIT extends AbstractResourceTest {

    private HiveSource hiveSource;

    @Autowired
    private HiveSourceService hiveSouceService;

    @BeforeEach
    void setup() {
        url = getBaseUrl() + V1 + CMM + DEFINITION + "/hive-source";
        hiveSource = hiveSource(getRandomString());
    }

    @Test
    void create() {
        var created = requestSpecWithBody(hiveSource)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", HiveSource.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(hiveSource.getName());
    }

    @Test
    void getById() {
        var created = hiveSouceService.create(hiveSource);
        var hiveSourceId = created.getId();

        var retrieved = requestSpec()
                .when().get(url + '/' + hiveSourceId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", HiveSource.class);

        assertThat(retrieved.getId()).isEqualTo(hiveSourceId);
        assertThat(retrieved.getName()).isEqualTo(hiveSource.getName());
    }

    @Test
    void update() {
        var created = hiveSouceService.create(hiveSource);
        var hiveSourceId = created.getId();

        var updateRequest = created.toBuilder().name(getRandomString()).build();

        var updated = requestSpecWithBody(updateRequest)
                .when().patch(url + '/' + hiveSourceId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", HiveSource.class);

        assertThat(updated.getId()).isEqualTo(hiveSourceId);
        assertThat(updated.getName()).isEqualTo(updateRequest.getName());
    }

    @Test
    void delete() {
        var created = hiveSouceService.create(hiveSource);
        var hiveSourceId = created.getId();

        requestSpec()
                .when().delete(url + '/' + hiveSourceId)
                .then().statusCode(HttpStatus.NO_CONTENT.value());

        requestSpec()
                .when().get(url + '/' + hiveSourceId)
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }
}