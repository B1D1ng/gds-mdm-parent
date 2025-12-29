package com.ebay.behavior.gds.mdm.udf.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.UDFMM;
import static org.assertj.core.api.Assertions.assertThat;

class UdfStubResourceIT extends AbstractResourceTest {

    private UdfStub udfStub;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + UDFMM + "/udf-stub";
        udfStub = TestModelUtils.udfStub(1L);
    }

    @Test
    void create() {
        var created = requestSpecWithBody(udfStub)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", UdfStub.class);

        assertThat(created.getId()).isNotNull();
    }
}
