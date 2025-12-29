package com.ebay.behavior.gds.mdm.signal.mockResource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.service.BusinessTagService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.businessTagNotification;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.BUSINESS_TAGS;

class BusinessTagResourceIT extends AbstractResourceTest {

    @MockitoBean
    private BusinessTagService service;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + BUSINESS_TAGS;
    }

    @Test
    void businessTags() {
        var notification = businessTagNotification().setStatus("OK");

        requestSpecWithBody(notification)
                .when().post(url)
                .then().statusCode(HttpStatus.ACCEPTED.value());
    }

    @Test
    void businessTags_invalidStatus() {
        var notification = businessTagNotification().setStatus("FAILED");

        requestSpecWithBody(notification)
                .when().post(url)
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
