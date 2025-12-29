package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.contract.model.LdmViewSink;
import com.ebay.behavior.gds.mdm.contract.service.LdmViewSinkService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.ldmViewSink;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class LdmViewSinkResourceIT extends AbstractResourceTest {

    private LdmViewSink ldmViewSink;

    @Autowired
    private LdmViewSinkService ldmViewSinkService;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + CMM + DEFINITION + "/ldm-view-sink";
        ldmViewSink = ldmViewSink(getRandomString());
    }

    @Test
    void create() {
        var created = requestSpecWithBody(ldmViewSink)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", LdmViewSink.class);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(ldmViewSink.getName());
        assertThat(created.getViewId()).isEqualTo(ldmViewSink.getViewId());
    }

    @Test
    void getById() {
        var createdLvs = ldmViewSinkService.create(ldmViewSink);
        var ldmViewSinkId = createdLvs.getId();

        var retrieved = requestSpec()
                .when().get(url + '/' + ldmViewSinkId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", LdmViewSink.class);

        assertThat(retrieved.getId()).isEqualTo(ldmViewSinkId);
        assertThat(retrieved.getName()).isEqualTo(ldmViewSink.getName());
        assertThat(retrieved.getViewId()).isEqualTo(ldmViewSink.getViewId());
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void update() {
        var created = ldmViewSinkService.create(ldmViewSink);
        var ldmViewSinkId = created.getId();
        created.setName(getRandomString());

        var updated = requestSpecWithBody(created)
                .when().patch(url + '/' + ldmViewSinkId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", LdmViewSink.class);

        assertThat(updated.getId()).isEqualTo(ldmViewSinkId);
        assertThat(updated.getName()).isEqualTo(created.getName());
        assertThat(updated.getViewId()).isEqualTo(created.getViewId());
    }

    @Test
    void delete() {
        var created = ldmViewSinkService.create(ldmViewSink);
        var ldmViewSinkId = created.getId();
        requestSpec()
                .when().delete(url + '/' + ldmViewSinkId)
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void delete_notFound() {
        requestSpec()
                .when().delete(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }
}
