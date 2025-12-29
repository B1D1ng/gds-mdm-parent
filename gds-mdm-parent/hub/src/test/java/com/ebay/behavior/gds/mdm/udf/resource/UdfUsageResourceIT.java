package com.ebay.behavior.gds.mdm.udf.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfUsage;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.UDFMM;
import static org.assertj.core.api.Assertions.assertThat;

class UdfUsageResourceIT extends AbstractResourceTest {

    private UdfUsage udfUsage;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + UDFMM + "/udf-usage";
        udfUsage = TestModelUtils.udfUsage(1L);
    }

    @Test
    void create() {
        var created = requestSpecWithBody(udfUsage)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", UdfUsage.class);

        assertThat(created.getId()).isNotNull();
    }
}
