package com.ebay.behavior.gds.mdm.signal.mockResource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalExportService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.METADATA;

class StagedSignalExportResourceIT extends AbstractResourceTest {

    @MockitoBean
    private StagedSignalExportService stagedSignalExportService;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + METADATA + "/export/signal";
    }

    @Test
    void exportSignals() {
        requestSpecWithBody("{}")
                .when().post(url)
                .then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void cleanupExportedSignals() {
        String cleanupUrl = url + "/cleanup";

        requestSpecWithBody("{}")
                .when().post(cleanupUrl)
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }
}