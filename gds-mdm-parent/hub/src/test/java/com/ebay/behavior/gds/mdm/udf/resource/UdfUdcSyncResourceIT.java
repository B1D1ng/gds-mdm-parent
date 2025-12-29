package com.ebay.behavior.gds.mdm.udf.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.UDC;

class UdfUdcSyncResourceIT extends AbstractResourceTest {

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + UDC + "/udf/1";
    }

    @Test
    void syncUdf() {
        requestSpec()
                .when().put(url)
                .then().statusCode(HttpStatus.OK.value());
    }
}
